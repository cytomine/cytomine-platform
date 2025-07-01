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

import datetime
import json

from pims.formats.utils.structures.metadata import (
    ImageChannel, ImageMetadata, Metadata, MetadataStore,
    MetadataType
)


def test_metadatatype():
    assert MetadataType.STRING.name == "STRING"
    assert MetadataType.INTEGER.name == "INTEGER"
    assert MetadataType.BOOLEAN.name == "BOOLEAN"
    assert MetadataType.DECIMAL.name == "DECIMAL"
    assert MetadataType.JSON.name == "JSON"


def test_metadata():
    data = [
        (MetadataType.STRING, "b"),
        (MetadataType.INTEGER, 2),
        (MetadataType.DECIMAL, 2.2),
        (MetadataType.BOOLEAN, True),
        (MetadataType.LIST, [3, 4]),
        (MetadataType.JSON, json.loads('{"a":2}')),
        (MetadataType.DATETIME, datetime.datetime.now()),
        (MetadataType.DATE, datetime.date(2020, 1, 1)),
        (MetadataType.TIME, datetime.time(10, 30, 3))

    ]

    for item in data:
        dtype, value = item
        m = Metadata("test", value, "namespace")
        assert m.key == "test"
        assert m.value == value
        assert m.metadata_type == dtype
        assert m.namespace == "NAMESPACE"
        assert m.namespaced_key == "NAMESPACE.test"

        m = Metadata("test", value)
        assert m.key == "test"
        assert m.value == value
        assert m.metadata_type == dtype
        assert m.namespace == ""
        assert m.namespaced_key == "test"


def test_metadatastore():
    ms = MetadataStore()
    assert len(ms) == 0

    ms.set("a", "b")
    assert ms.get("a") == Metadata("a", "b")
    assert ms.get_value("a") == "b"

    ms.set("test.a", 2)
    assert len(ms) == 2
    assert ms.get("test.a") == Metadata("a", 2, "test")
    assert ms.get_value("test.a") == 2

    ms.set("test.a", 3, "test2")
    assert len(ms) == 3
    assert ms.get("test2.test.a") == Metadata("test.a", 3, "test2")

    ms = MetadataStore()
    ms.set("test.a", 2)
    d = dict(TEST=dict(a=Metadata("a", 2, "test")))
    assert str(ms) == str(d)
    assert repr(ms) == str(d)
    assert next(iter(ms)) == next(iter(d))
    assert list(ms.keys()) == ["TEST.a"]
    # for a, b in zip(ms.values(), d.values()):
    #     assert a == b
    # for a, b in zip(ms.items(), d.items()):
    #     assert a == b
    assert "b" not in ms
    assert "TEST" in ms
    assert Metadata("a", 2, "test") in ms


def test_to_metadata_store():
    imd = ImageMetadata()
    imd.width = 10
    imd.height = 100
    imd.objective.nominal_magnification = 2
    imd.microscope.model = "foo"
    imd.set_channel(ImageChannel(index=1))
    imd.n_concrete_channels = 1

    store = imd.to_metadata_store(MetadataStore())
    assert store.get_value("image.width") == 10
    assert store.get_value("image.height") == 100
    assert store.get_value("objective.nominal_magnification") == 2
    assert store.get_value("channel[1].index") == 1
    assert store.get_value("microscope.model") == "foo"
