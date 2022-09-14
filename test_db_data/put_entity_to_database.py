from typing import List, Dict

import psycopg2

langs = {"ru": 1, "en": 2}
# TODO придумать что-то с соединением, чтобы не закрывать/открывать на каждую сущность
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


def put(data: Dict, table: str, pk_columns: List[str], simple_columns: List[str]):
    conn = psycopg2.connect(dbname='seeker', user='dev',
                            password='dev', host='localhost')
    cursor = conn.cursor()
    if 'lang' in data:
        data['lang'] = langs[data['lang']]
    if is_entity_present(table, data, pk_columns):
        if len(simple_columns) > 0:
            update_entity(table, data, simple_columns, pk_columns)
    else:
        insert_entity(table, data, simple_columns + pk_columns)
    cursor.close()
    conn.close()
