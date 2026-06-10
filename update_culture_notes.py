import psycopg2
import os

try:
    conn = psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", "5433"),
        dbname=os.getenv("DB_NAME", "fnb_db"),
        user=os.getenv("DB_USER", "fnb_user"),
        password=os.getenv("DB_PASS", "fnb_pass")
    )
    conn.autocommit = True
    cursor = conn.cursor()
    cursor.execute("ALTER TABLE menu.restaurant_profile ADD COLUMN IF NOT EXISTS local_culture_notes VARCHAR(500);")
    print("Successfully added local_culture_notes to menu.restaurant_profile")
except Exception as e:
    print(f"Error: {e}")
finally:
    if 'conn' in locals() and conn:
        cursor.close()
        conn.close()
