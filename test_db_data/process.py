import json
from typing import List, Dict

import psycopg2

langs = {"ru": 1, "en": 2}

conn = psycopg2.connect(dbname='seeker', user='dev',
                        password='dev', host='localhost')
cursor = conn.cursor()


def is_entity_present(table: str, data: Dict, pk_columns: List[str]) -> bool:
    pk_template = ' and '.join([f'{column}=%s' for column in pk_columns])
    fields = [str(data[column]) for column in pk_columns]
    sql = f'SELECT * FROM {table} WHERE {pk_template}'
    cursor.execute(sql, fields)
    return len(cursor.fetchall()) > 0


def insert_entity(table: str, data: Dict, columns: List[str]):
    sql_columns = ', '.join(columns)
    template = ', '.join(['%s' for _ in range(len(columns))])
    fields = [str(data[column]) for column in columns]
    sql = f'INSERT INTO {table} ({sql_columns}) VALUES ({template})'
    cursor.execute(sql, fields)
    conn.commit()


def update_entity(table: str, data: Dict, update_columns: List[str], pk_columns: List[str]):
    pk_template = ' and '.join([f'{column}=%s' for column in pk_columns])
    update_template = ', '.join([f'{column}=%s' for column in update_columns])
    fields = [str(data[column]) for column in update_columns] + [str(data[column]) for column in pk_columns]
    sql = f'UPDATE {table} SET {update_template} WHERE {pk_template}'
    cursor.execute(sql, fields)
    conn.commit()


with open('test_db_data/data/events.json', encoding='utf-8') as json_file:
    data = json.load(json_file)
    for element in data:
        print(element)
        if is_entity_present('event', element, ['id']):
            pass
            # TODO для событий не надо
        else:
            insert_entity('event', element, ['id'])
        locales = element['locale']
        for locale in locales:
            locale['event_id'] = element['id']
            if 'lang' in locale:
                locale['lang'] = langs[locale['lang']]
            if is_entity_present('event_locale', locale, ['event_id', 'lang']):
                update_entity('event_locale', locale, ['name'], ['event_id', 'lang'])
            else:
                insert_entity('event_locale', locale, ['event_id', 'lang', 'name'])

cursor.close()
conn.close()
