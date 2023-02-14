


from unittest import TestCase
from bootstrapper.deployment.env_generator import RandomUUIDGenerator, OpenSSLGenerator, SecretGenerator
from bootstrapper.deployment.env_generator import UnrecognizedGenerationField, InvalidAutoGenerationData
from tests.util import UUID_PATTERN


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
    self.assertRegex(generator.resolve("random_uuid"), UUID_PATTERN)


class TestOpensslGenerator(TestCase):
  def testValidate(self):
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
    self.assertEqual(generator, generator.validate({"type": "openssl", "length": 10}))

  def testMethodKey(self):
    generator = OpenSSLGenerator()
    self.assertEqual("openssl", generator.method_key)

  def testResolve(self):
    generator = OpenSSLGenerator()
    field = {"type": "openssl", "length": 10}
    # not sure how 'length' affects final string length
    self.assertRegex(generator.resolve(field), rf".+")


class TestSercretGenerator(TestCase):
  def testValidate(self):
    generator = SecretGenerator()
    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate("aa")

    with self.assertRaises(UnrecognizedGenerationField):
      generator.validate({"type": "sec"})

    with self.assertRaises(InvalidAutoGenerationData):
      generator.validate({"type": "secret", "length": -1})
    
    self.assertEqual(generator, generator.validate({"type": "secret"}))
    self.assertEqual(generator, generator.validate({"type": "secret", "length": 10}))

  def testMethodKey(self):
    generator = SecretGenerator()
    self.assertEqual("secret", generator.method_key)

  def testResolve(self):
    generator = SecretGenerator()
    self.assertEqual(len(generator.resolve({"type": "secret", "length": 1})), 1)
    self.assertEqual(len(generator.resolve({"type": "secret", "length": 10})), 10)
    self.assertEqual(len(generator.resolve({"type": "secret", "length": 20})), 20)
    self.assertEqual(generator.resolve({
      "type": "secret",
      "length": 10,
      "blacklist": generator.base_alphabet[1:]
    }), generator.base_alphabet[0] * 10)
    self.assertEqual(generator.resolve({
      "type": "secret",
      "length": 10,
      "whitelist": "a"
    }), "a" * 10)
    print(generator.base_alphabet)