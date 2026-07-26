import base64
import unittest
import warnings
from pathlib import Path
from unittest.mock import patch

warnings.filterwarnings("ignore", message="Using `httpx` with `starlette.testclient` is deprecated")

from fastapi.testclient import TestClient

import main


class PaddleOcrApiTest(unittest.TestCase):

    def setUp(self):
        self.client = TestClient(main.app)
        self.image_base64 = base64.b64encode(b"image-bytes").decode("ascii")

    def test_batch_ocr_returns_per_image_status_and_keeps_order(self):
        def fake_predict(_model, image_paths):
            self.assertEqual(2, len(image_paths))
            return [
                ["invoice number"],
                [],
            ]

        with patch.object(main, "_get_ocr_model", return_value=object()), \
                patch.object(main, "_predict", side_effect=fake_predict), \
                patch.object(main.logger, "warning") as warning_log:
            response = self.client.post("/ocr/batch", json={
                "images": [
                    {"id": "img-1", "imageBase64": self.image_base64, "fileName": "one.png"},
                    {"id": "img-2", "imageBase64": self.image_base64, "fileName": "two.png"},
                    {"id": "img-3", "imageBase64": "bad-base64", "fileName": "three.png"},
                ]
            })

        warning_log.assert_called_once()
        self.assertEqual(200, response.status_code)
        body = response.json()
        self.assertEqual(3, len(body["items"]))
        self.assertEqual("img-1", body["items"][0]["id"])
        self.assertEqual("SUCCESS", body["items"][0]["status"])
        self.assertEqual("invoice number", body["items"][0]["text"])
        self.assertEqual(["invoice number"], body["items"][0]["results"])
        self.assertGreaterEqual(body["items"][0]["elapsedMs"], 0)
        self.assertEqual("img-2", body["items"][1]["id"])
        self.assertEqual("EMPTY", body["items"][1]["status"])
        self.assertEqual("", body["items"][1]["text"])
        self.assertEqual("img-3", body["items"][2]["id"])
        self.assertEqual("FAILED", body["items"][2]["status"])
        self.assertEqual("", body["items"][2]["text"])
        self.assertIn("Invalid imageBase64", body["items"][2]["error"])
        self.assertEqual("SUCCESS", body["status"])

    def test_batch_ocr_predicts_valid_images_in_single_model_call_and_keeps_order(self):
        predict_inputs = []

        def fake_predict(_model, image_paths):
            predict_inputs.append(image_paths)
            self.assertIsInstance(image_paths, list)
            self.assertEqual(2, len(image_paths))
            self.assertTrue(all(Path(image_path).exists() for image_path in image_paths))
            return [
                {"rec_texts": ["first image"]},
                {"rec_texts": ["second image"]},
            ]

        with patch.object(main, "_get_ocr_model", return_value=object()), \
                patch.object(main, "_predict", side_effect=fake_predict):
            response = self.client.post("/ocr/batch", json={
                "images": [
                    {"id": "img-1", "imageBase64": self.image_base64, "fileName": "one.png"},
                    {"id": "img-2", "imageBase64": self.image_base64, "fileName": "two.png"},
                ]
            })

        self.assertEqual(200, response.status_code)
        self.assertEqual(1, len(predict_inputs))
        body = response.json()
        self.assertEqual("SUCCESS", body["status"])
        self.assertEqual("img-1", body["items"][0]["id"])
        self.assertEqual("SUCCESS", body["items"][0]["status"])
        self.assertEqual("first image", body["items"][0]["text"])
        self.assertEqual("img-2", body["items"][1]["id"])
        self.assertEqual("SUCCESS", body["items"][1]["status"])
        self.assertEqual("second image", body["items"][1]["text"])

    def test_batch_ocr_rejects_empty_images(self):
        response = self.client.post("/ocr/batch", json={"images": []})

        self.assertEqual(422, response.status_code)

    def test_create_ocr_model_uses_performance_options(self):
        with patch.object(main, "PaddleOCR") as paddle_ocr, \
                patch.object(main, "OCR_TEXT_RECOGNITION_BATCH_SIZE", 12, create=True), \
                patch.object(main, "OCR_TEXTLINE_ORIENTATION_BATCH_SIZE", 8, create=True), \
                patch.object(main, "OCR_CPU_THREADS", 6, create=True), \
                patch.object(main, "OCR_DEVICE", "cpu", create=True), \
                patch.object(main, "OCR_USE_TEXTLINE_ORIENTATION", False, create=True):
            main._create_ocr_model()

        paddle_ocr.assert_called_once()
        _, kwargs = paddle_ocr.call_args
        self.assertIn("text_recognition_batch_size", kwargs)
        self.assertIn("textline_orientation_batch_size", kwargs)
        self.assertIn("cpu_threads", kwargs)
        self.assertIn("device", kwargs)
        self.assertEqual(12, kwargs["text_recognition_batch_size"])
        self.assertEqual(8, kwargs["textline_orientation_batch_size"])
        self.assertEqual(6, kwargs["cpu_threads"])
        self.assertEqual("cpu", kwargs["device"])
        self.assertFalse(kwargs["use_textline_orientation"])

    def test_extract_texts_ignores_paddleocr_metadata(self):
        raw_result = [{
            "res": {
                "input_path": "/tmp/input.png",
                "text_det_params": {
                    "limit_type": "min",
                },
                "text_type": "general",
                "rec_texts": ["Snail OCR Test"],
            },
        }]

        self.assertEqual(["Snail OCR Test"], main._extract_texts(raw_result))


if __name__ == "__main__":
    unittest.main()
