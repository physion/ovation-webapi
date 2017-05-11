import argparse
import json

from getpass import getpass

from tqdm import tqdm
from cloudant.client import Cloudant
from cloudant.adapters import Replay429Adapter

LINKS = 'links'
COLLABORATION_ROOTS = '_collaboration_roots'


def transfer_project(client, db_name,
                     project=None,
                     dest_org=None):

    db = client[db_name]

    proj = db[project]
    current_org = int(proj['organization'])

    end_point = '{0}/{1}'.format(client.server_url, '{}/_design/api/_view/by_project'.format(db_name))
    params = {'include_docs': 'false',
              'keys': json.dumps([project]) }

    # https://ovation-io-dev.cloudant.com/staging/_design/api/_view/by_project?limit=20&reduce=false&include_docs=true&keys=%5B%22002e2984-186d-41a2-a064-2c09a9b37faf%22%5D
    # Issue the request
    response = client.r_session.get(end_point, params=params)

    # Display the response content
    total_docs = int(response.json()['total_rows'])

    for doc in tqdm(db, total=total_docs, unit='doc'):
        doc['organization'] = dest_org
        doc.save()


# Copy design doc...
# curl -X COPY https://ovation-io-dev:$PASSWORD@ovation-io-dev.cloudant.com/staging/_design/api-warm -H "Destination: _design/api?rev=12-2f8efce525a10bdc79754071bf8abd26"

def main():
    parser = argparse.ArgumentParser(prog='python transfer_project.py')
    parser.add_argument('-u', '--user', help='Cloudant user email')
    parser.add_argument('-p', '--password', help='Cloudant password')
    parser.add_argument('-a', '--account', help='Cloudant account')
    parser.add_argument('database', help='Cloudant database')
    parser.add_argument('project', help='Project UUID')
    parser.add_argument('dest_org', help='Destination Organization Id')

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
        transfer_project(client, args.database,
                         project=args.project,
                         dest_org=args.dest_org)
    finally:
        client.disconnect()


if __name__ == '__main__':
    main()
