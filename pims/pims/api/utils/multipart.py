from __future__ import annotations

import typing
from pathlib import Path

import aiofiles
from multipart.multipart import parse_options_header, MultipartParser
from starlette.datastructures import Headers
from starlette.formparsers import MultiPartParser, MultiPartException, _user_safe_decode
from starlette.requests import ClientDisconnect

from pims.api.exceptions import UploadCanceledException


class FastSinglePartParser(MultiPartParser):
    """
    Faster version of Starlette MultipartParser specialized to fastly write chunks to a file asynchronously.
    It only works for multipart data with a single part (file).

    We know that, besides the first chunks where it is useful to retrieve the headers "Content-Disposition" and
    the "Content-Type" (and not write them in the file) , all the other chunks will be bytes of the image to upload and
    can be written right away in a file on disk.
    Therefore, we can get inspired by the parse() function of FastAPI and, by assuming that there is only one file per
    request (we do not handle multiple file upload) and no other key-value pairs, we can parse the first chunks until
    the headers are finished to retrieve the headers "Content-Disposition" and the "Content-Type" (to get the filename)
    by calling process_chunks_headers(). Once the headers are process, we can directly write the bytes into a file.
    """

    def __init__(self, filepath: Path, headers: Headers, stream: typing.AsyncGenerator[bytes, None]) -> None:
        super().__init__(headers, stream, max_files=1)
        self._filepath = filepath
        self._filename = ""

    def on_headers_finished(self) -> None:
        disposition, options = parse_options_header(
            self._current_part.content_disposition
        )
        try:
            self._current_part.field_name = _user_safe_decode(
                options[b"name"], self._charset
            )
        except KeyError:
            raise MultiPartException(
                'The Content-Disposition header field "name" must be ' "provided."
            )
        if b"filename" in options:
            self._current_files += 1
            if self._current_files > self.max_files:
                raise MultiPartException(
                    f"Too many files. Maximum number of files is {self.max_files}."
                )
            self._filename = _user_safe_decode(options[b"filename"], self._charset)
        else:
            self._current_fields += 1
            if self._current_fields > self.max_fields:
                raise MultiPartException(
                    f"Too many fields. Maximum number of fields is {self.max_fields}."
                )

    def on_part_data(self, data: bytes, start: int, end: int) -> None:
        message_bytes = data[start:end]
        self._file_parts_to_write.append((self._current_part, message_bytes))

    async def parse(self) -> str:
        # Parse the Content-Type header to get the multipart boundary.
        _, params = parse_options_header(self.headers["Content-Type"])
        charset = params.get(b"charset", "utf-8")
        if isinstance(charset, bytes):
            charset = charset.decode("latin-1")
        self._charset = charset
        try:
            boundary = params[b"boundary"]
        except KeyError:
            raise MultiPartException("Missing boundary in multipart.")

        # Callbacks dictionary.
        callbacks = {
            "on_part_data": self.on_part_data,
            "on_header_field": self.on_header_field,
            "on_header_value": self.on_header_value,
            "on_header_end": self.on_header_end,
            "on_headers_finished": self.on_headers_finished,
            "on_end": self.on_end,
        }

        # Create the parser.
        parser = MultipartParser(boundary, callbacks)

        async with aiofiles.open(self._filepath, mode='wb') as file:
            try:
                # Feed the parser with data from the request.
                async for chunk in self.stream:
                    if self._current_files == 1:
                        await file.write(chunk)
                    else:
                        parser.write(chunk)

                        for _, data in self._file_parts_to_write:
                            await file.write(data)
                        self._file_parts_to_write.clear()
            except MultiPartException as exc:
                # Close all the files if there was an error.
                await file.close()
                raise exc
            except ClientDisconnect:
                await file.close()
                raise UploadCanceledException()

        parser.finalize()
        return self._filename
