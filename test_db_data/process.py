import json
from typing import List, Dict

import psycopg2

conn = psycopg2.connect(dbname='seeker', user='dev',
                        password='dev', host='localhost')
cursor = conn.cursor()


def is_entity_present(table: str, data: Dict, pk_columns: List[str]) -> bool:
    pk_values = {(pk, data[pk]) for pk in pk_columns}
    pk_list = [f'{str(k)}={str(v)}' for k, v in pk_values]
    pk_condition = ' and '.join(pk_list)
    sql = f'SELECT * FROM {table} WHERE {pk_condition}'
    cursor.execute(sql)
    return len(cursor.fetchall()) > 0


def insert_entity(table: str, data: Dict, columns: List[str]):
    sql_columns = ', '.join(columns)
    sql_values = ', '.join([str(data[column]) for column in columns])
    sql = f'INSERT INTO {table} ({sql_columns}) VALUES ({sql_values})'
    cursor.execute(sql)
    conn.commit()


def update_entity(table: str, data: Dict, columns: List[str], pk_columns: List[str]):
    pass


with open('test_db_data/data/events.json', encoding='utf-8') as json_file:
    data = json.load(json_file)
    for element in data:
        print(element)
        if is_entity_present('event', element, ['id']):
            pass
        else:
            insert_entity('event', element, ['id'])

cursor.close()
conn.close()
