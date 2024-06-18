#  * Copyright (c) 2020-2021. Authors: see NOTICE file.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
import base64
import hashlib
import hmac

from starlette.requests import Request

from pims.api.exceptions import AuthenticationException
from pims.config import get_settings


def lreplace(string, old, new):
    return new + string[len(old):] if string.startswith(old) else string


def parse_authorization_header(raw_headers):
    auth = raw_headers.get("authorization")
    if auth is None or not auth.startswith("CYTOMINE") \
            or ' ' not in auth or ':' not in auth:
        raise AuthenticationException("Auth failed")

    public_key = auth[(auth.index(" ") + 1):(auth.index(":"))]
    signature = auth[(auth.index(":") + 1):]
    return public_key, signature


def parse_request_token(request: Request):
    headers = request.headers

    md5 = headers.get("content-MD5", "")
    date = headers.get("date", headers.get("dateFull", ""))

    content_type = headers.get(
        "content-type-full",
        headers.get(
            "Content-Type",
            headers.get("content-type", "")
        )
    )
    content_type = "" if content_type == "null" else content_type

    query_string = request.url.query
    query_string = "?" + query_string if query_string is not None else ""

    client_url_path = lreplace(request.url.path, get_settings().api_base_path, "")

    message = "{}\n{}\n{}\n{}\n{}{}".format(
        request.method, md5, content_type,
        date, client_url_path, query_string
    )
    return message


def sign_token(private_key, token):
    return base64.b64encode(
        hmac.new(
            bytes(private_key, 'utf-8'),
            token.encode('utf-8'),
            hashlib.sha1
        ).digest()
    ).decode('utf-8')
