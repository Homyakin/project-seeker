import threading

import process_file

threads = [
    threading.Thread(target=process_file.process, args=['test_db_data/data/events.json'])
]

for i in threads:
    i.start()

for i in threads:
    i.join()
