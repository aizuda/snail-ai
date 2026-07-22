import unittest
from pathlib import Path


class SetupScriptTest(unittest.TestCase):

    def test_setup_script_documents_huggingface_mirror_fallback(self):
        script = Path(__file__).resolve().parents[2] / "setup-docling-api-env.bat"
        content = script.read_text(encoding="utf-8")

        self.assertIn("DOCLING_HF_ENDPOINT", content)
        self.assertIn("HF_ENDPOINT", content)
        self.assertIn("https://hf-mirror.com", content)
        self.assertIn("Retrying with HuggingFace mirror", content)
        self.assertIn("layout tableformer rapidocr", content)


if __name__ == "__main__":
    unittest.main()
