import json
from typing import Dict

import put_entity_to_database
import validation


def process_file(env: str):
    with open(f'data/{env}/menu_items.json', encoding='utf-8') as json_file:
        menu_items = json.load(json_file)
        for menu_item in menu_items:
            process_menu_item(menu_item)


def process_menu_item(menu_item: Dict):
    print(f'Processing menu item with id {menu_item["id"]}')

    put_entity_to_database.put(menu_item, table='menu_item', pk_columns=['id'], simple_columns=['price', 'is_available', 'category_id'])

    if 'locale' in menu_item:
        process_locale(menu_item['locale'], menu_item["id"])
    else:
        raise Exception(f'Event with id {menu_item["id"]} must contain locale')


def process_locale(locales: Dict, menu_item_id: int):
    validation.validate_locales(locales)
    for locale in locales:
        locale['menu_item_id'] = menu_item_id
        put_entity_to_database.put(
            locale,
            table='menu_item_locale',
            pk_columns=['menu_item_id', 'language_id'],
            simple_columns=['name', 'consume_template']
        )
