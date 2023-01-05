


from unittest import TestCase
from bootstrapper.env_generator import RandomUUIDGenerator, OpenSSLGenerator
from bootstrapper.env_generator import UnrecognizedGenerationField, InvalidAutoGenerationData


class TestUUIDGenerator(TestCase):
  def testValidate(self):
    generator = RandomUUIDGenerator()

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate("aa")

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate({})

    self.assertEqual(generator, generator.validate("random_uuid"))

  def testMethodKey(self):
    generator = RandomUUIDGenerator()
    self.assertEqual("random_uuid", generator.method_key)

  def testResolve(self):
    generator = RandomUUIDGenerator()
    self.assertRegex(generator.resolve("random_uuid"), r"[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}")


class TestOpensslGenerator(TestCase):
  def testValidate(self):
    generator = OpenSSLGenerator()

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate("aa")

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate({"type": "open"})

    with self.assertRaises(InvalidAutoGenerationData):
      generator.validate({"type": "openssl", "base64": True, "length": -1})

    with self.assertRaises(InvalidAutoGenerationData):
      generator.validate({"type": "openssl", "base64": 25, "length": 10})

    self.assertEqual(generator, generator.validate({"type": "openssl"}))
    self.assertEqual(generator, generator.validate({"type": "openssl", "base64": True, "length": 10}))

  def testMethodKey(self):
    generator = OpenSSLGenerator()
    self.assertEqual("openssl", generator.method_key)

  def testResolve(self):
    generator = OpenSSLGenerator()
    field = {"type": "openssl", "base64": True, "length": 10}
    self.assertRegex(generator.resolve(field), rf".+")