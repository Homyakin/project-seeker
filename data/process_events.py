import json
from typing import Dict

import database
import validation


def process_file(env: str):
    with open(f'data/{env}/events.json', encoding='utf-8') as json_file:
        events = json.load(json_file)
        for event in events:
            process_event(event)


def process_event(event: Dict):
    print(f'Processing event with id {event["id"]}')
    validation.validate_event(event)

    if 'raid' in event:
        event_type = 1
        process_type = process_raid
        type_object = event['raid']
    else:
        raise Exception('Unknown event type')

    event['type_id'] = event_type

    database.put(event, table='event', pk_columns=['id'], simple_columns=['duration', 'type_id', 'is_enabled'])
    process_type(type_object, event['id'])

    if 'locale' in event:
        process_locale(event['locale'], event["id"])
    else:
        raise Exception(f'Event with id {event["id"]} must contain locale')


def process_raid(raid: Dict, event_id: int):
    raid['event_id'] = event_id
    database.put(
        raid,
        table='raid',
        pk_columns=['event_id'],
        simple_columns=['template_id', 'name']
    )


def process_locale(locales: Dict, event_id: int):
    validation.validate_locales(locales)
    for locale in locales:
        locale['event_id'] = event_id
        database.put(
            locale,
            table='event_locale',
            pk_columns=['event_id', 'language_id'],
            simple_columns=['intro', 'description']
        )
