import os
import re

BACKEND_DIR = r"d:\srcDOAN\backend"
OUTPUT_FILE = r"d:\srcDOAN\api_endpoints_detailed.md"

class_request_mapping_re = re.compile(r'@RequestMapping\s*\(\s*["\']([^"\']+)["\']\s*\)')
method_signature_re = re.compile(r'public\s+(?:<[^>]+>\s+)?([\w<>,?\[\]\s]+)\s+(\w+)\s*\(([^)]*)\)')

# Regex to find class or record definitions
class_def_re = re.compile(r'public\s+(?:abstract\s+)?(?:class|record)\s+(\w+)')
# Regex to find fields in class
field_re = re.compile(r'private\s+([\w<>,?\[\]\s]+)\s+(\w+)\s*;')
# Regex to find record components (rough)
record_comp_re = re.compile(r'(?:public\s+)?(?:record\s+\w+\s*\()([^)]+)\)')

dto_map = {}

def build_dto_map():
    for root, dirs, files in os.walk(BACKEND_DIR):
        for file in files:
            if file.endswith(".java") and ("dto" in root.lower() or "request" in file.lower() or "response" in file.lower()):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Find classes
                for class_match in class_def_re.finditer(content):
                    class_name = class_match.group(1)
                    fields = []
                    
                    # Extract class body
                    start_idx = class_match.end()
                    # A simplistic way: just scan the rest of the file or until next class
                    # It's rough but works for standard DTOs
                    
                    # Normal class fields
                    for field_match in field_re.finditer(content):
                        # Filter to ensure it's likely in this class (simplification: assume 1 file = 1 main class)
                        field_type = field_match.group(1).strip()
                        field_name = field_match.group(2).strip()
                        if "static" not in field_type and "final" not in field_type:
                            fields.append((field_type, field_name))
                            
                    # Record fields
                    rec_match = record_comp_re.search(content)
                    if rec_match and "record " + class_name in content:
                        comps = rec_match.group(1).split(',')
                        for comp in comps:
                            comp = comp.strip()
                            if comp:
                                parts = comp.split()
                                if len(parts) >= 2:
                                    fields.append((parts[-2], parts[-1]))
                                    
                    dto_map[class_name] = fields

def expand_dto(type_str):
    # Remove generics like ResponseEntity<ApiResponse<...>>
    # We want the innermost type
    inner_type = type_str
    matches = re.findall(r'<([^>]+)>', type_str)
    if matches:
        inner_type = matches[-1] # Usually the innermost
        
    # Sometimes it's List<X>
    if "List<" in type_str:
        m = re.search(r'List<([^>]+)>', type_str)
        if m: inner_type = m.group(1)

    # Sometimes ApiResponse<X>
    if "ApiResponse<" in type_str:
        m = re.search(r'ApiResponse<([^>]+)>', type_str)
        if m: inner_type = m.group(1)
        
    # Check if inner_type has generic
    m = re.search(r'^([^<]+)', inner_type)
    base_class = inner_type
    if m:
        base_class = m.group(1).strip()

    if base_class in dto_map and dto_map[base_class]:
        fields_str = "<br>".join([f"- `{t}` {n}" for t, n in dto_map[base_class]])
        return f"**{base_class}**<br>{fields_str}"
    
    return type_str

def parse_java_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    base_path = ""
    class_mapping = class_request_mapping_re.search(content)
    if class_mapping:
        base_path = class_mapping.group(1)

    endpoints = []
    parts = re.split(r'(@(?:Get|Post|Put|Delete|Patch)Mapping)', content)
    
    i = 1
    while i + 1 < len(parts):
        annotation = parts[i]
        block = parts[i+1]
        i += 2
        
        mapping_match = re.search(r'^\s*(?:\(\s*["\']([^"\']*)["\']\s*\))?', block)
        sub_path = ""
        if mapping_match and mapping_match.group(1):
            sub_path = mapping_match.group(1)
            
        full_path = base_path + sub_path
        full_path = full_path.replace('//', '/')
        
        method_type = annotation.replace('@', '').replace('Mapping', '').upper()
        
        sig_match = method_signature_re.search(block)
        if sig_match:
            return_type = sig_match.group(1).strip()
            method_name = sig_match.group(2).strip()
            params = sig_match.group(3).strip()
            
            # Clean up params
            params_clean = []
            if params:
                for p in params.split(','):
                    p = p.strip()
                    p = re.sub(r'@\w+(?:\([^)]*\))?\s+', '', p)
                    parts = p.split()
                    if len(parts) >= 2:
                        ptype = parts[-2]
                        pname = parts[-1]
                        # Expand DTO if it's a request body
                        expanded = expand_dto(ptype)
                        if "**" in expanded:
                            params_clean.append(f"{pname}: {expanded}")
                        else:
                            params_clean.append(f"{ptype} {pname}")
                    else:
                        params_clean.append(p)
            
            endpoints.append({
                'method': method_type,
                'path': full_path,
                'name': method_name,
                'return_type': return_type,
                'return_expanded': expand_dto(return_type),
                'params': params_clean
            })
            
    return endpoints

def main():
    print("Building DTO map...")
    build_dto_map()
    print(f"Found {len(dto_map)} DTOs.")
    
    services = {}
    for root, dirs, files in os.walk(BACKEND_DIR):
        for file in files:
            if file.endswith("Controller.java"):
                filepath = os.path.join(root, file)
                rel_path = os.path.relpath(filepath, BACKEND_DIR)
                service_name = rel_path.split(os.sep)[0]
                
                endpoints = parse_java_file(filepath)
                if endpoints:
                    if service_name not in services:
                        services[service_name] = []
                    services[service_name].append({
                        'controller': file,
                        'endpoints': endpoints
                    })

    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write("# Chi tiết API Endpoints & Cấu trúc Dữ liệu (Req/Res)\n\n")
        
        for service, controllers in services.items():
            f.write(f"## 🏢 {service.upper()}\n\n")
            for ctrl in controllers:
                f.write(f"### 📄 {ctrl['controller']}\n\n")
                f.write("| Method | Endpoint | Request (Body/Params) | Response (Return) |\n")
                f.write("|--------|----------|-----------------------|-------------------|\n")
                
                for ep in ctrl['endpoints']:
                    method = ep['method']
                    path = ep['path']
                    req = "<br><br>".join(ep['params']) if ep['params'] else "*None*"
                    res = ep['return_expanded'].replace('\n', '<br>')
                    
                    f.write(f"| `{method}` | `{path}` | {req} | {res} |\n")
                f.write("\n")

    print(f"Successfully wrote {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
