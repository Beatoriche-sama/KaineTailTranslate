import json
import sys
import time

import requests
from translatepy.translators import DeeplTranslate, GoogleTranslateV2, YandexTranslate

source = sys.argv[1]
lang = sys.argv[2]
choice = sys.argv[3]


class JsonDeeplTranslator:
    def translate(self, source_text, language):
        request = requests.post(
            "https://www2.deepl.com/jsonrpc",
            json={
                "jsonrpc": "2.0",
                "method": "LMT_handle_jobs",
                "params": {
                    "jobs": [{
                        "kind": "default",
                        "raw_en_sentence": source_text,
                        "raw_en_context_before": [],
                        "raw_en_context_after": [],
                        "preferred_num_beams": 4,
                        "quality": "fast"
                    }],
                    "lang": {
                        "source_lang_user_selected": "auto",
                        "target_lang": language
                    },
                    "priority": -1,
                    "commonJobParams": {},
                    "timestamp": int(round(time.time() * 1000))
                },
                "id": 40890008
            }
        )
        return json.dumps(request.json())


serviceMap = {'DeepLSoupFree': DeeplTranslate(),
              'DeepLJsonFree': JsonDeeplTranslator(),
              'Google': GoogleTranslateV2(),
              'Yandex': YandexTranslate()
              }
translator = serviceMap[choice]
print(translator.translate(source, lang))

sys.exit(0)
