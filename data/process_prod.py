import threading

import process_events

threads = [
    threading.Thread(target=process_events.process_file, args=['project-seeker-prod-data'])
]

for i in threads:
    i.start()

for i in threads:
    i.join()
