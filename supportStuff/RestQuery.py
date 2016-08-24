import requests
import sys
appid = #TODO go to http://api.digitalpodcast.com/get_appid.html to get an application ID
url = 'http://api.digitalpodcast.com/v2r/search/'

val = 'events @ RAND'
#val = sys.argv[1]
def main():
    payload = {'appid':appid, \
               'keywords': val, 
               'results': '10',\
               'sort': 'rel', \
               'format':'rss'}
    print payload
    r = requests.get(url, params=payload)
    print r.text
    return r




if __name__=='__main__':
    main()
