from typing import List, Dict

import psycopg2

langs = {"ru": 1, "en": 2}
# TODO придумать что-то с соединением, чтобы не закрывать/открывать на каждую сущность
conn = psycopg2.connect(dbname='seeker', user='dev',
                        password='dev', host='localhost')


def is_entity_present(table: str, data: Dict, pk_columns: List[str]) -> bool:
    cursor = conn.cursor()
    pk_template = ' and '.join([f'{column}=%s' for column in pk_columns])
    fields = [str(data[column]) for column in pk_columns]
    sql = f'SELECT * FROM {table} WHERE {pk_template}'
    cursor.execute(sql, fields)
    result = len(cursor.fetchall())
    cursor.close()
    return result > 0


def insert_entity(table: str, data: Dict, columns: List[str]):
    cursor = conn.cursor()
    sql_columns = ', '.join(columns)
    template = ', '.join(['%s' for _ in range(len(columns))])
    fields = [data[column] for column in columns]
    sql = f'INSERT INTO {table} ({sql_columns}) VALUES ({template})'
    cursor.execute(sql, fields)
    cursor.close()
    conn.commit()


def update_entity(table: str, data: Dict, update_columns: List[str], pk_columns: List[str]):
    cursor = conn.cursor()
    pk_template = ' and '.join([f'{column}=%s' for column in pk_columns])
    update_template = ', '.join([f'{column}=%s' for column in update_columns])
    fields = [data[column] for column in update_columns] + [data[column] for column in pk_columns]
    sql = f'UPDATE {table} SET {update_template} WHERE {pk_template}'
    cursor.execute(sql, fields)
    cursor.close()
    conn.commit()


def put(data: Dict, table: str, pk_columns: List[str], simple_columns: List[str]):
    if 'language_id' in data:
        data['language_id'] = langs[data['language_id']]
    if is_entity_present(table, data, pk_columns):
        if len(simple_columns) > 0:
            update_entity(table, data, simple_columns, pk_columns)
    else:
        insert_entity(table, data, simple_columns + pk_columns)


def get_value(table: str, search_data: Dict, required_columns: List[str]) -> List:
    cursor = conn.cursor()
    search_template = ' and '.join([f"{column}='{value}'" for column, value in search_data.items()])
    result_columns = ', '.join(required_columns)
    sql = f'SELECT {result_columns} FROM {table} WHERE {search_template}'
    cursor.execute(sql)
    result = cursor.fetchall()
    if len(result) > 1:
        print("WARN: getting value has more than 1 result")
    cursor.close()
    return result[0]
