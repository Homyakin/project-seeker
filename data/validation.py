from typing import Dict

required_langs = ['ru', 'en']


def validate_locales(locales: Dict):
    languages = []
    for locale in locales:
        languages.append(locale['language_id'])

    for required_lang in required_langs:
        if required_lang not in languages:
            raise Exception(f'Missing {required_lang} language')


event_types = ['raid']


def validate_event(event: Dict):
    types_in_event = 0
    for event_type in event_types:
        if event_type in event:
            types_in_event += 1
    if types_in_event != 1:
        raise Exception(f'Event must contain 1 type instead of {types_in_event}')