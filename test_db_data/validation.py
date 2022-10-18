from typing import Dict

required_langs = ['ru', 'en']


def validate_locales(locales: Dict):
    languages = []
    for locale in locales:
        languages.append(locale['lang'])

    for required_lang in required_langs:
        if required_lang not in languages:
            raise Exception(f'Missing {required_lang} language')
