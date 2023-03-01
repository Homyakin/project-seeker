import threading

import process_events
import process_menu_items

threads = [
    threading.Thread(target=process_events.process_file, args=['project-seeker-prod-data']),
    threading.Thread(target=process_menu_items.process_file, args=['project-seeker-prod-data'])
]

for i in threads:
    i.start()

for i in threads:
    i.join()
