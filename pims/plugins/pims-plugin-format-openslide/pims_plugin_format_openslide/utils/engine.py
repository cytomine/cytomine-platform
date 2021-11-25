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

from pyvips import Image as VIPSImage

from pims.formats import AbstractFormat
from pims.formats.utils.engines.vips import VipsParser, VipsReader, get_vips_field
from pims.formats.utils.structures.metadata import ImageMetadata, MetadataStore
from pims.formats.utils.structures.pyramid import Pyramid
from pims.utils import UNIT_REGISTRY
from pims.utils.types import parse_float, parse_int


def cached_vips_openslide_file(format: AbstractFormat) -> VIPSImage:
    return format.get_cached(
        '_vipsos', VIPSImage.openslideload, str(format.path)
    )


class OpenslideVipsParser(VipsParser):
    def parse_main_metadata(self) -> ImageMetadata:
        imd = super().parse_main_metadata()

        # Openslide (always ?) gives image with alpha channel
        if imd.n_channels in (2, 4):
            imd.n_channels -= 1
            imd.n_channels_per_read = imd.n_channels

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        image = cached_vips_openslide_file(self.format)

        imd = super(OpenslideVipsParser, self).parse_known_metadata()
        mppx = parse_float(get_vips_field(image, 'openslide.mpp-x'))
        if mppx is not None:
            imd.physical_size_x = mppx * UNIT_REGISTRY("micrometers")
        mppy = parse_float(get_vips_field(image, 'openslide.mpp-y'))
        if mppy:
            imd.physical_size_y = mppy * UNIT_REGISTRY("micrometers")

        imd.objective.nominal_magnification = parse_float(
            get_vips_field(image, 'openslide.objective-power')
        )

        for associated in ('macro', 'thumbnail', 'label'):
            if associated in get_vips_field(image, 'slide-associated-images', []):
                head = VIPSImage.openslideload(
                    str(self.format.path), associated=associated
                )
                imd_associated = getattr(imd, f'associated_{associated[:5]}')
                imd_associated.width = head.width
                imd_associated.height = head.height
                imd_associated.n_channels = head.bands
        return imd

    def parse_raw_metadata(self) -> MetadataStore:
        image = cached_vips_openslide_file(self.format)

        store = super().parse_raw_metadata()
        for key in image.get_fields():
            if '.' in key:
                store.set(key, get_vips_field(image, key))
        return store

    def parse_pyramid(self) -> Pyramid:
        image = cached_vips_openslide_file(self.format)

        pyramid = Pyramid()
        n_levels = parse_int(get_vips_field(image, 'openslide.level-count'))
        if n_levels is None:
            return super(OpenslideVipsParser, self).parse_pyramid()

        for level in range(n_levels):
            prefix = f'openslide.level[{level}].'
            width = parse_int(get_vips_field(image, prefix + 'width'))
            height = parse_int(get_vips_field(image, prefix + 'height'))
            pyramid.insert_tier(
                width, height,
                (parse_int(get_vips_field(image, prefix + 'tile-width', width)),
                 parse_int(get_vips_field(image, prefix + 'tile-height', height)))
            )

        return pyramid


class OpenslideVipsReader(VipsReader):
    def read_thumb(self, out_width, out_height, precomputed=False, **other):
        if precomputed:
            imd = self.format.full_imd
            if imd.associated_thumb.exists:
                return VIPSImage.openslideload(
                    str(self.format.path), associated='thumbnail'
                ).flatten()

        return super().read_thumb(out_width, out_height, **other)

    def read_window(self, region, out_width, out_height, **other):
        out_size = (out_width, out_height)
        tier = self.format.pyramid.most_appropriate_tier(region, out_size)
        region = region.scale_to_tier(tier)

        level_page = VIPSImage.openslideload(
            str(self.format.path), level=tier.level
        )
        return level_page.extract_area(
            region.left, region.top, region.width, region.height
        ).flatten()

    def read_tile(self, tile, **other):
        tier = tile.tier
        level_page = VIPSImage.openslideload(
            str(self.format.path), level=tier.level
        )

        # There is no direct access to underlying tiles in vips
        # But the following computation match vips implementation so that only
        # the tile that has to be read is read.
        # https://github.com/jcupitt/tilesrv/blob/master/tilesrv.c#L461
        return level_page.extract_area(
            tile.left, tile.top, tile.width, tile.height
        ).flatten()

    def read_label(self, out_width, out_height, **other):
        imd = self.format.full_imd
        if imd.associated_label.exists:
            return VIPSImage.openslideload(
                str(self.format.path), associated='label'
            ).flatten()
        return None

    def read_macro(self, out_width, out_height, **other):
        imd = self.format.full_imd
        if imd.associated_macro.exists:
            return VIPSImage.openslideload(
                str(self.format.path), associated='macro'
            ).flatten()
        return None
