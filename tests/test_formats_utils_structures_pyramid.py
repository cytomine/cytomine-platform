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
from collections import defaultdict
from pims.formats.utils.structures.pyramid import Pyramid, PyramidTier, normalized_pyramid
from pims.processing.region import Region
from unittest import TestCase

from pims.api.utils.output_parameter import safeguard_output_dimensions

def test_pyramid_tier():
    tier = PyramidTier(1000, 2000, 256, Pyramid())
    assert tier.n_pixels == 1000 * 2000
    assert tier.factor == (1.0, 1.0)


def test_pyramid():
    p = Pyramid()
    p.insert_tier(1000, 2000, 256)
    assert p.n_levels == 1
    assert p.max_level == 0
    assert p.n_zooms == 1
    assert p.max_zoom == 0

    p.insert_tier(100, 200, 256)
    assert p.n_levels == 2
    assert p.get_tier_at_level(1).n_pixels == 100 * 200
    assert p.get_tier_at_level(1).level == 1
    assert p.get_tier_at_level(1).zoom == 0

    p.insert_tier(500, 1000, 256)
    assert p.n_levels == 3
    assert p.get_tier_at_level(1).n_pixels == 500 * 1000
    assert p.get_tier_at_level(1).level == 1
    assert p.get_tier_at_level(1).zoom == 1
    assert p.get_tier_at_level(2).n_pixels == 100 * 200
    assert p.get_tier_at_level(2).level == 2
    assert p.get_tier_at_level(2).zoom == 0


def test_pyramid_tier_indexes():
    tier = PyramidTier(1000, 2000, 256, Pyramid())
    assert tier.max_tx == 4
    assert tier.max_ty == 8
    assert tier.max_ti == 32

    assert tier.txty2ti(0, 0) == 0
    assert tier.txty2ti(3, 7) == 31

    assert tier.ti2txty(0) == (0, 0)
    assert tier.ti2txty(31) == (3, 7)

    assert tier.get_txty_tile(0, 0) == Region(0, 0, 256, 256)
    assert tier.get_txty_tile(3, 7) == Region(1792, 768, 1000 - 768, 2000 - 1792)

    assert tier.get_ti_tile(0) == Region(0, 0, 256, 256)
    assert tier.get_ti_tile(31) == Region(1792, 768, 1000 - 768, 2000 - 1792)

class TestTierSelection(TestCase):
  def test_odd_dim(self):
    results = {
      0: {"level": 0, "chosen_tier_level": 0, "factor": 1.0, "chosen_tier_factor": 1.0},
      1: {"level": 1, "chosen_tier_level": 1, "factor": 1.999986309621598, "chosen_tier_factor": 1.999986309621598},
      2: {"level": 2, "chosen_tier_level": 2, "factor": 3.9999470470657252, "chosen_tier_factor": 3.99999819207462},
      3: {"level": 3, "chosen_tier_level": 3, "factor": 7.999572778283946, "chosen_tier_factor": 8.000215439200636},
      4: {"level": 4, "chosen_tier_level": 4, "factor": 15.997860468473235, "chosen_tier_factor": 16.00043087840127},
      5: {"level": 5, "chosen_tier_level": 5, "factor": 31.994084756943614, "chosen_tier_factor": 32.004367597550605},
      6: {"level": 6, "chosen_tier_level": 6, "factor": 63.967618786687154, "chosen_tier_factor": 64.00873519510121},
      7: {"level": 7, "chosen_tier_level": 7, "factor": 127.90908277283188, "chosen_tier_factor": 128.07362534814945},
      8: {"level": 8, "chosen_tier_level": 8, "factor": 255.49003610768318, "chosen_tier_factor": 256.1472506962989},
      9: {"level": 9, "chosen_tier_level": 9, "factor": 510.98007221536636, "chosen_tier_factor": 512.7786292920924},
      10: {"level": 10, "chosen_tier_level": 10, "factor": 1018.4129901960785, "chosen_tier_factor": 1025.5572585841849},
    }

    ## Create VSI pyramid based on image VSI `089-03 S1.vsi` 156418x73043 (odd dimensions)
    nb_of_levels = 11
    tile_size = 512
    vsi_pyramid = Pyramid()
    width, height = 156418, 73043
    vsi_pyramid.insert_tier(width, height, tile_size)
    for level in range(1, nb_of_levels):
        # Stick to the way the vsi_pyramid tier are created in PIMS to avoid any difference between level calculation
        width, height = round(width / 2), round(height / 2)
        vsi_pyramid.insert_tier(width, height, tile_size)

    ## Create normalized pyramid based on image VSI `089-03 S1.vsi`
    normalized_vsi_pyramid = normalized_pyramid(156418, 73043)
    for reference_tier_index in range(0,normalized_vsi_pyramid.n_levels):

      normalized_factor = normalized_vsi_pyramid.get_tier_at_level(reference_tier_index).average_factor
      chosen_tier = vsi_pyramid.most_appropriate_tier_for_downsample_factor(normalized_factor)

      self.assertAlmostEqual(reference_tier_index, results[reference_tier_index]["level"])
      self.assertAlmostEqual(chosen_tier.level, results[reference_tier_index]["chosen_tier_level"])
      self.assertAlmostEqual(normalized_factor, results[reference_tier_index]["factor"])
      self.assertAlmostEqual(chosen_tier.average_factor, results[reference_tier_index]["chosen_tier_factor"])


  def test_even_dim(self):
    results = {
      0: {"level": 0, "chosen_tier_level": 0, "factor": 1.0, "chosen_tier_factor": 1.0},
      1: {"level": 1, "chosen_tier_level": 1, "factor": 2.0, "chosen_tier_factor": 2.0},
      2: {"level": 2, "chosen_tier_level": 2, "factor": 4.0, "chosen_tier_factor": 4.0},
      3: {"level": 3, "chosen_tier_level": 3, "factor": 7.999780965940204, "chosen_tier_factor": 8.000219058050384},
      4: {"level": 4, "chosen_tier_level": 4, "factor": 15.99868593955322, "chosen_tier_factor": 16.000438116100767},
      5: {"level": 5, "chosen_tier_level": 5, "factor": 31.99737187910644, "chosen_tier_factor": 32.00438212094654},
      6: {"level": 6, "chosen_tier_level": 6, "factor": 63.98073555166375, "chosen_tier_factor": 64.00876424189308},
      7: {"level": 7, "chosen_tier_level": 7, "factor": 127.9614711033275, "chosen_tier_factor": 128.07368421052632},
      8: {"level": 8, "chosen_tier_level": 8, "factor": 255.6993006993007, "chosen_tier_factor": 256.14736842105265},
      9: {"level": 9, "chosen_tier_level": 9, "factor": 510.9803007450066, "chosen_tier_factor": 512.7788824449967},
      10: {"level": 10, "chosen_tier_level": 10, "factor": 1018.4133986928105, "chosen_tier_factor": 1025.5577648899935},
    }

    ## Create VSI pyramid based on image VSI `009-01 S1.vsi` 156416x73044 (even dimensions)
    nb_of_levels = 11
    tile_size = 512
    vsi_pyramid = Pyramid()
    width, height = 156416, 73044
    vsi_pyramid.insert_tier(width, height, tile_size)
    for level in range(1, nb_of_levels):
        # Stick to the way the vsi_pyramid tier are created in PIMS to avoid any difference between level calculation
        width, height = round(width / 2), round(height / 2)
        vsi_pyramid.insert_tier(width, height, tile_size)

    ## Create normalized pyramid based on image VSI `009-01 S1.vsi`
    normalized_vsi_pyramid = normalized_pyramid(156416, 73044)
    for reference_tier_index in range(0,normalized_vsi_pyramid.n_levels):

      normalized_factor = normalized_vsi_pyramid.get_tier_at_level(reference_tier_index).average_factor
      chosen_tier = vsi_pyramid.most_appropriate_tier_for_downsample_factor(normalized_factor)

      self.assertAlmostEqual(reference_tier_index, results[reference_tier_index]["level"])
      self.assertAlmostEqual(chosen_tier.level, results[reference_tier_index]["chosen_tier_level"])
      self.assertAlmostEqual(normalized_factor, results[reference_tier_index]["factor"])
      self.assertAlmostEqual(chosen_tier.average_factor, results[reference_tier_index]["chosen_tier_factor"])