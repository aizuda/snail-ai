import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from docling.datamodel.pipeline_options import RapidOcrOptions

import main


class DoclingApiTest(unittest.TestCase):

    def test_build_pipeline_options_uses_onnxruntime_ocr_backend(self):
        options = main._build_pipeline_options(do_ocr=True, do_table_structure=True)

        self.assertTrue(options.do_ocr)
        self.assertIsInstance(options.ocr_options, RapidOcrOptions)
        self.assertEqual("onnxruntime", options.ocr_options.backend)
        self.assertEqual(["chinese"], options.ocr_options.lang)

    def test_build_pipeline_options_uses_local_docling_model_cache(self):
        options = main._build_pipeline_options(do_ocr=True, do_table_structure=True)

        self.assertEqual(main.DOCLING_MODEL_DIR, options.artifacts_path)

    def test_build_pipeline_options_generates_picture_images_when_embedded(self):
        options = main._build_pipeline_options(
            do_ocr=True,
            do_table_structure=True,
            image_export_mode="embedded",
        )

        self.assertTrue(options.generate_picture_images)

    def test_build_pipeline_options_skips_picture_images_when_placeholder(self):
        options = main._build_pipeline_options(
            do_ocr=True,
            do_table_structure=True,
            image_export_mode="placeholder",
        )

        self.assertFalse(options.generate_picture_images)

    def test_resolve_image_ref_mode_defaults_to_embedded(self):
        self.assertEqual(main.ImageRefMode.EMBEDDED, main._resolve_image_ref_mode(None))
        self.assertEqual(main.ImageRefMode.EMBEDDED, main._resolve_image_ref_mode("bad-value"))
        self.assertEqual(main.ImageRefMode.PLACEHOLDER, main._resolve_image_ref_mode("placeholder"))

    def test_build_pipeline_options_keeps_ocr_disabled(self):
        options = main._build_pipeline_options(do_ocr=False, do_table_structure=True)

        self.assertFalse(options.do_ocr)


if __name__ == "__main__":
    unittest.main()
