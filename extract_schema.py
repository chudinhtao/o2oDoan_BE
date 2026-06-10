import os
import re

entity_dir = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\entity'

for filename in os.listdir(entity_dir):
    if filename.endswith('.java'):
        path = os.path.join(entity_dir, filename)
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        table_match = re.search(r'@Table\s*\([^)]*name\s*=\s*"([^"]+)"', content)
        table_name = table_match.group(1) if table_match else filename.replace('.java', '')
        
        print(f'\n--- Table: {table_name} (Entity: {filename}) ---')
        
        lines = content.split('\n')
        for i, line in enumerate(lines):
            line = line.strip()
            if line.startswith('private ') or line.startswith('protected '):
                print(f'Field: {line}')
            elif '@Column' in line or '@ManyToOne' in line or '@Enumerated' in line or '@OneToMany' in line:
                print(f'  Constraint/Rel: {line}')
