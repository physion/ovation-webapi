import argparse

from getpass import getpass

from tqdm import tqdm
from cloudant.client import Cloudant
from cloudant.adapters import Replay429Adapter
from cloudant.result import Result

LINKS = 'links'
COLLABORATION_ROOTS = '_collaboration_roots'


def migrate(client, db_name):
    db = client[db_name]
    end_point = '{0}/{1}'.format(client.server_url, '{}/_all_docs'.format(db_name))
    params = {'include_docs': 'false', 'limit': 1}

    # Issue the request
    response = client.r_session.get(end_point, params=params)

    # Display the response content
    total_docs = int(response.json()['total_rows'])

    n_missing = 0
    for doc in tqdm(db, total=total_docs, unit='doc'):
        if 'type' in doc:
            doc['organization'] = "0"
            doc.save()


def main():
    parser = argparse.ArgumentParser(prog='python add_organizations.py')
    parser.add_argument('-u', '--user', help='Cloudant user email')
    parser.add_argument('-p', '--password', help='Cloudant password')
    parser.add_argument('-a', '--account', help='Cloudant account')
    parser.add_argument('database', help='Cloudant database')

    args = parser.parse_args()

    if args.user is None:
        args.user = input('Email: ')

    if args.password is None:
        args.password = getpass('Password: ')

    if args.account is None:
        args.account = input('Cloudant account: ')

    client = Cloudant(args.user, args.password,
                      account=args.account,
                      connect=True,
                      auto_renew=True,
                      adapter=Replay429Adapter(retries=10, initialBackoff=0.01))

    try:
        migrate(client, args.database)
    finally:
        client.disconnect()


if __name__ == '__main__':
    main()
