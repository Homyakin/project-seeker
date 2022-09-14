import json
from typing import Dict

import put_entity_to_database


def process(file_path: str):
    with open(file_path, encoding='utf-8') as json_file:
        data = json.load(json_file)
        table = data['table']
        for element in data['data']:
            print(element)
            put_entity_to_database.put(element, table, data['pk_columns'], data['simple_columns'])
            if 'nested_data' in data:
                for k, v in data['nested_data'].items():
                    process_nested(element, element[k], v)


# TODO добавить валидацию на проверку обязательных локалей
def process_nested(parent: Dict, children: Dict, child_info: Dict):
    for element in children:
        for parent_field, child_field in child_info['parent_mapping'].items():
            element[child_field] = parent[parent_field]
        print(element)
        put_entity_to_database.put(element, child_info['table'], child_info['pk_columns'], child_info['simple_columns'])
        if 'nested_data' in child_info:
            for k, v in child_info['nested_data']:
                process_nested(element, element[k], v)
