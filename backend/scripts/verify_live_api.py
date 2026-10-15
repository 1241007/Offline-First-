import asyncio
import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(backend_dir))

from httpx import AsyncClient, ASGITransport
from app.main import app

async def test_live_api():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url='http://test') as client:
        # 1. Test C++ course
        res = await client.get('/api/v1/courses/cpp-programming')
        assert res.status_code == 200, f'Status {res.status_code}'
        data = res.json()
        print('Course:', data['name'], f"({data['id']})")
        print('Modules count:', len(data['modules']))

        # Check first module, first lesson (overview video)
        les0 = data['modules'][0]['lessons'][0]
        print('  Lesson 0:', les0['title'])
        print('    Reading URL:', les0['resourceUrl'])
        print('    Reading Provider:', les0['resourceProvider'])
        print('    Video URL:', les0['videoUrl'])
        print('    Video Provider:', les0['videoProvider'])

        assert les0['videoUrl'] == 'https://www.youtube.com/watch?v=vLnPwxZdW4Y'
        assert les0['videoProvider'] == 'freeCodeCamp.org'
        assert les0['resourceUrl'] is not None

        # Check second lesson (reading only - videoUrl is null)
        les1 = data['modules'][0]['lessons'][1]
        print('  Lesson 1:', les1['title'])
        print('    Reading URL:', les1['resourceUrl'])
        print('    Reading Provider:', les1['resourceProvider'])
        print('    Video URL:', les1['videoUrl'])
        assert les1['videoUrl'] is None
        assert les1['resourceUrl'] is not None

        # 2. Test DSA course
        res_dsa = await client.get('/api/v1/courses/data-structures-algorithms')
        assert res_dsa.status_code == 200
        dsa_data = res_dsa.json()
        dsa_les0 = dsa_data['modules'][0]['lessons'][0]
        print('DSA Lesson 0 Video:', dsa_les0['videoUrl'])
        print('DSA Lesson 0 Video Provider:', dsa_les0['videoProvider'])
        assert dsa_les0['videoUrl'] == 'https://www.youtube.com/watch?v=RBSGKlAvoiM'

        # 3. Test canonical Android alias lookup "c-cpp"
        res_alias = await client.get('/api/v1/courses/c-cpp')
        assert res_alias.status_code == 200
        alias_data = res_alias.json()
        assert alias_data['id'] == data['id']
        assert alias_data['modules'][0]['lessons'][0]['videoUrl'] == 'https://www.youtube.com/watch?v=vLnPwxZdW4Y'

        print('\n[LIVE API VERIFICATION SUCCESSFUL 100%]')

if __name__ == '__main__':
    asyncio.run(test_live_api())
