import json
from typing import Dict

import put_entity_to_database
import validation


def process_file(env: str):
    with open(f'data/{env}/events.json', encoding='utf-8') as json_file:
        events = json.load(json_file)
        for event in events:
            process_event(event)


def process_event(event: Dict):
    print(f'Processing event with id {event["id"]}')
    validation.validate_event(event)

    if 'boss' in event:
        event_type = 1
        process_type = process_boss
        type_object = event['boss']
    else:
        raise Exception('Unknown event type')

    event['type'] = event_type

    put_entity_to_database.put(event, table='event', pk_columns=['id'], simple_columns=['duration', 'type', 'is_enabled'])
    process_type(type_object, event['id'])

    if 'locale' in event:
        process_locale(event['locale'], event["id"])
    else:
        raise Exception(f'Event with id {event["id"]} must contain locale')


def process_boss(boss: Dict, event_id: int):
    personage = boss['personage']
    put_entity_to_database.put(
        personage,
        table='personage',
        pk_columns=['id'],
        simple_columns=['name', 'level', 'current_exp', 'attack', 'defense', 'health', 'strength', 'agility', 'wisdom', 'last_health_change']
    )
    boss['event_id'] = event_id
    boss['personage_id'] = personage['id']
    put_entity_to_database.put(
        boss,
        table='boss',
        pk_columns=['event_id'],
        simple_columns=['personage_id']
    )


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
