from unittest import TestCase

from cytomine_installer.deployment.env_generator import (
    InvalidAutoGenerationData, OpenSSLGenerator, RandomUUIDGenerator,
    SecretGenerator, UnrecognizedGenerationField)
from tests.util import UUID_PATTERN


class TestUUIDGenerator(TestCase):
  def test_validate(self):
    generator = RandomUUIDGenerator()

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate("aa")

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate({})

    self.assertEqual(generator, generator.validate("random_uuid"))

  def test_method_key(self):
    generator = RandomUUIDGenerator()
    self.assertEqual("random_uuid", generator.method_key)

  def test_resolve(self):
    generator = RandomUUIDGenerator()
    self.assertRegex(generator.resolve("random_uuid"), UUID_PATTERN)


class TestOpensslGenerator(TestCase):
  def test_validate(self):
    generator = OpenSSLGenerator()

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate("aa")

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate({"type": "open"})

    with self.assertRaises(InvalidAutoGenerationData):
      generator.validate({"type": "openssl", "length": -1})

    with self.assertRaises(InvalidAutoGenerationData):
      generator.validate({"type": "openssl", "length": 0})

    self.assertEqual(generator, generator.validate({"type": "openssl"}))
    self.assertEqual(
      generator, generator.validate({"type": "openssl", "length": 10})
    )

  def test_method_key(self):
    generator = OpenSSLGenerator()
    self.assertEqual("openssl", generator.method_key)

  def test_resolve(self):
    generator = OpenSSLGenerator()
    field = {"type": "openssl", "length": 10}
    # not sure how 'length' affects final string length
    self.assertRegex(generator.resolve(field), r".+")


class TestSercretGenerator(TestCase):
  def test_validate(self):
    generator = SecretGenerator()
    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate("aa")

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate({"type": "sec"})

    with self.assertRaises(InvalidAutoGenerationData):
      generator.validate({"type": "secret", "length": -1})

    self.assertEqual(generator, generator.validate({"type": "secret"}))
    self.assertEqual(
      generator, generator.validate({"type": "secret", "length": 10})
    )

  def test_method_key(self):
    generator = SecretGenerator()
    self.assertEqual("secret", generator.method_key)

  def test_resolve(self):
    generator = SecretGenerator()
    self.assertEqual(len(generator.resolve({"type": "secret", "length": 1})), 1)
    self.assertEqual(len(generator.resolve({"type": "secret", "length": 10})), 10)
    self.assertEqual(len(generator.resolve({"type": "secret", "length": 20})), 20)
    self.assertEqual(
      generator.resolve(
        {
          "type": "secret",
          "length": 10,
          "blacklist": generator.base_alphabet[1:],
        }
      ),
      generator.base_alphabet[0] * 10,
    )
    self.assertEqual(
      generator.resolve({"type": "secret", "length": 10, "whitelist": "a"}),
      "a" * 10,
    )
