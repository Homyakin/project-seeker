import json
from typing import Dict

import put_entity_to_database
import validation


def process_file():
    with open('test_db_data/data/events.json', encoding='utf-8') as json_file:
        events = json.load(json_file)
        for event in events:
            process_event(event)


def process_event(event: Dict):
    print(f'Processing event with id {event["id"]}')
    put_entity_to_database.put(event, table='event', pk_columns=['id'], simple_columns=['duration'])

    if 'locale' in event:
        process_locale(event['locale'], event["id"])
    else:
        error_text = f'Event with id {event["id"]} must contain locale'
        print(error_text)
        raise Exception(error_text)


def process_locale(locales: Dict, event_id: int):
    validation.validate_locales(locales)
    for locale in locales:
        locale['event_id'] = event_id
        put_entity_to_database.put(
            locale,
            table='event_locale',
            pk_columns=['event_id', 'lang'],
            simple_columns=['name', 'description']
        )
