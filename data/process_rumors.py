import json
from typing import Dict

import database
import validation

table = 'rumor'


def process_file(env: str):
    with open(f'data/{env}/rumors.json', encoding='utf-8') as json_file:
        rumors = json.load(json_file)
        for rumor in rumors:
            process_rumor(rumor)


def process_rumor(rumor: Dict):
    print(f'Processing rumor with code {rumor["code"]}')

    database.put(rumor, table=table, pk_columns=['code'], simple_columns=['is_available'])

    if 'locale' in rumor:
        process_locale(
            rumor['locale'],
            database.get_value(table=table, search_data={'code': rumor['code']}, required_columns=['id'])[0]
        )
    else:
        raise Exception(f'Rumor with code {rumor["code"]} must contain locale')


def process_locale(locales: Dict, rumor_id: int):
    validation.validate_locales(locales)
    for locale in locales:
        locale['rumor_id'] = rumor_id
        database.put(
            locale,
            table='rumor_locale',
            pk_columns=['rumor_id', 'language_id'],
            simple_columns=['text']
        )
