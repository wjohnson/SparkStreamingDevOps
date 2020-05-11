import json
import logging
import os
import random
import time
import string

from dotenv import load_dotenv

from azure.eventhub import EventHubProducerClient, EventData

def random_text(n=3):
    return ''.join([string.ascii_lowercase[random.randint(0, 25)] for i in range(n)])

if __name__ == "__main__":
    load_dotenv()

    logger = logging.getLogger("azure")

    connection_str = os.environ.get("EHUB_SEND_CONN_STR")
    eventhub_name = os.environ.get("EHUB_SEND_NAME")

    while True:
        client = EventHubProducerClient.from_connection_string(connection_str, eventhub_name=eventhub_name)
        event_data_batch = client.create_batch()
        try:
            terms = random.randint(1,50)
            tokens = random.randint(0,5)
            for i in range(terms):
                message = ' '.join([random_text() for i in range(tokens)])
                event_data_batch.add(EventData(message))
        except ValueError:
            pass

        with client:
            client.send_batch(event_data_batch)
            print("Sent {} messages.".format(len(event_data_batch)))
        
        time_to_sleep = random.randint(0,25)
        
        print("Sleeping for {}".format(time_to_sleep))
        time.sleep(time_to_sleep)
        