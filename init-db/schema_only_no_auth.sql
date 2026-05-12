WARNING:  database "fnb_db" has a collation version mismatch
DETAIL:  The database was created using collation version 2.41, but the operating system provides version 2.36.
HINT:  Rebuild all objects in this database that use the default collation and run ALTER DATABASE fnb_db REFRESH COLLATION VERSION, or build PostgreSQL with the right library version.
--
-- PostgreSQL database dump
--

\restrict DTT9chsWrQimdQNP7nYjvllDNzhOAJg2aCqipPTPQRjGXB9OiK6KeK1MPPfzpS6

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg12+1)
-- Dumped by pg_dump version 16.13 (Debian 16.13-1.pgdg12+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE ONLY orders.table_sessions DROP CONSTRAINT table_sessions_table_id_fkey;
ALTER TABLE ONLY orders.staff_calls DROP CONSTRAINT staff_calls_table_id_fkey;
ALTER TABLE ONLY orders.staff_calls DROP CONSTRAINT staff_calls_session_id_fkey;
ALTER TABLE ONLY orders.staff_calls DROP CONSTRAINT staff_calls_resolved_by_fkey;
ALTER TABLE ONLY orders.staff_calls DROP CONSTRAINT staff_calls_accepted_by_fkey;
ALTER TABLE ONLY orders.orders DROP CONSTRAINT orders_table_id_fkey;
ALTER TABLE ONLY orders.orders DROP CONSTRAINT orders_session_id_fkey;
ALTER TABLE ONLY orders.orders DROP CONSTRAINT orders_cashier_id_fkey;
ALTER TABLE ONLY orders.orders DROP CONSTRAINT orders_cancelled_by_fkey;
ALTER TABLE ONLY orders.order_tickets DROP CONSTRAINT order_tickets_order_id_fkey;
ALTER TABLE ONLY orders.order_ticket_items DROP CONSTRAINT order_ticket_items_ticket_id_fkey;
ALTER TABLE ONLY orders.order_ticket_items DROP CONSTRAINT order_ticket_items_served_by_fkey;
ALTER TABLE ONLY orders.order_ticket_items DROP CONSTRAINT order_ticket_items_cancelled_by_fkey;
ALTER TABLE ONLY orders.order_item_options DROP CONSTRAINT order_item_options_ticket_item_id_fkey;
ALTER TABLE ONLY orders.tables DROP CONSTRAINT fk_parent_table;
ALTER TABLE ONLY menu.promotion_targets DROP CONSTRAINT promotion_targets_promotion_id_fkey;
ALTER TABLE ONLY menu.promotion_schedules DROP CONSTRAINT promotion_schedules_promotion_id_fkey;
ALTER TABLE ONLY menu.promotion_requirements DROP CONSTRAINT promotion_requirements_promotion_id_fkey;
ALTER TABLE ONLY menu.promotion_bundle_items DROP CONSTRAINT promotion_bundle_items_promotion_id_fkey;
ALTER TABLE ONLY menu.promotion_bundle_items DROP CONSTRAINT promotion_bundle_items_item_id_fkey;
ALTER TABLE ONLY menu.menu_items DROP CONSTRAINT menu_items_category_id_fkey;
ALTER TABLE ONLY menu.item_options DROP CONSTRAINT item_options_group_id_fkey;
ALTER TABLE ONLY menu.item_option_groups DROP CONSTRAINT item_option_groups_item_id_fkey;
ALTER TABLE ONLY kds.kds_ticket_items DROP CONSTRAINT kds_ticket_items_prepared_by_fkey;
ALTER TABLE ONLY kds.kds_ticket_item_options DROP CONSTRAINT fk_kds_ticket_item_option;
ALTER TABLE ONLY kds.kds_ticket_items DROP CONSTRAINT fk_kds_ticket;
DROP INDEX orders.unq_active_order;
DROP INDEX orders.idx_ticket_items_status_done;
DROP INDEX orders.idx_staff_calls_resolved_by;
DROP INDEX orders.idx_staff_calls_pending;
DROP INDEX orders.idx_orders_cashier;
DROP INDEX orders.idx_order_items_served_by;
DROP INDEX orders.idx_audit_logs_user_id;
DROP INDEX orders.idx_audit_logs_target_id;
DROP INDEX orders.idx_audit_logs_action_name;
DROP INDEX orders.flyway_schema_history_s_idx;
DROP INDEX menu.idx_promotions_scope;
DROP INDEX menu.idx_promotions_code;
DROP INDEX menu.idx_promotions_active;
DROP INDEX menu.idx_promo_targets_type;
DROP INDEX menu.idx_promo_bundle_item;
DROP INDEX menu.idx_option_groups_item;
DROP INDEX menu.idx_menu_items_category;
DROP INDEX menu.idx_menu_items_available;
DROP INDEX menu.idx_item_options_group;
DROP INDEX menu.idx_categories_active;
DROP INDEX menu.flyway_schema_history_s_idx;
DROP INDEX kds.idx_kds_items_prepared_by;
DROP INDEX kds.flyway_schema_history_s_idx;
ALTER TABLE ONLY orders.tables DROP CONSTRAINT tables_qr_token_key;
ALTER TABLE ONLY orders.tables DROP CONSTRAINT tables_pkey;
ALTER TABLE ONLY orders.tables DROP CONSTRAINT tables_number_key;
ALTER TABLE ONLY orders.table_sessions DROP CONSTRAINT table_sessions_session_token_key;
ALTER TABLE ONLY orders.table_sessions DROP CONSTRAINT table_sessions_pkey;
ALTER TABLE ONLY orders.table_info DROP CONSTRAINT table_info_pkey;
ALTER TABLE ONLY orders.staff_calls DROP CONSTRAINT staff_calls_pkey;
ALTER TABLE ONLY orders.orders DROP CONSTRAINT orders_pkey;
ALTER TABLE ONLY orders.order_tickets DROP CONSTRAINT order_tickets_pkey;
ALTER TABLE ONLY orders.order_ticket_items DROP CONSTRAINT order_ticket_items_pkey;
ALTER TABLE ONLY orders.order_item_options DROP CONSTRAINT order_item_options_pkey;
ALTER TABLE ONLY orders.flyway_schema_history DROP CONSTRAINT flyway_schema_history_pk;
ALTER TABLE ONLY orders.audit_logs DROP CONSTRAINT audit_logs_pkey;
ALTER TABLE ONLY menu.restaurant_profile DROP CONSTRAINT restaurant_profile_pkey;
ALTER TABLE ONLY menu.promotions DROP CONSTRAINT promotions_pkey;
ALTER TABLE ONLY menu.promotions DROP CONSTRAINT promotions_code_key;
ALTER TABLE ONLY menu.promotion_targets DROP CONSTRAINT promotion_targets_pkey;
ALTER TABLE ONLY menu.promotion_schedules DROP CONSTRAINT promotion_schedules_pkey;
ALTER TABLE ONLY menu.promotion_requirements DROP CONSTRAINT promotion_requirements_pkey;
ALTER TABLE ONLY menu.promotion_bundle_items DROP CONSTRAINT promotion_bundle_items_pkey;
ALTER TABLE ONLY menu.menu_items DROP CONSTRAINT menu_items_pkey;
ALTER TABLE ONLY menu.item_options DROP CONSTRAINT item_options_pkey;
ALTER TABLE ONLY menu.item_option_groups DROP CONSTRAINT item_option_groups_pkey;
ALTER TABLE ONLY menu.flyway_schema_history DROP CONSTRAINT flyway_schema_history_pk;
ALTER TABLE ONLY menu.categories DROP CONSTRAINT categories_pkey;
ALTER TABLE ONLY menu.ai_semantic_cache DROP CONSTRAINT ai_semantic_cache_pkey;
ALTER TABLE ONLY kds.kds_tickets DROP CONSTRAINT kds_tickets_pkey;
ALTER TABLE ONLY kds.kds_ticket_items DROP CONSTRAINT kds_ticket_items_pkey;
ALTER TABLE ONLY kds.flyway_schema_history DROP CONSTRAINT flyway_schema_history_pk;
ALTER TABLE ONLY ai.knowledge_base DROP CONSTRAINT knowledge_base_pkey;
ALTER TABLE ONLY ai.admin_semantic_cache DROP CONSTRAINT admin_semantic_cache_pkey;
DROP TABLE orders.tables;
DROP TABLE orders.table_sessions;
DROP TABLE orders.table_info;
DROP TABLE orders.staff_calls;
DROP TABLE orders.orders;
DROP TABLE orders.order_tickets;
DROP TABLE orders.order_ticket_items;
DROP TABLE orders.order_item_options;
DROP TABLE orders.flyway_schema_history;
DROP TABLE orders.audit_logs;
DROP TABLE menu.restaurant_profile;
DROP TABLE menu.promotions;
DROP TABLE menu.promotion_targets;
DROP TABLE menu.promotion_schedules;
DROP TABLE menu.promotion_requirements;
DROP TABLE menu.promotion_bundle_items;
DROP TABLE menu.menu_items;
DROP TABLE menu.item_options;
DROP TABLE menu.item_option_groups;
DROP TABLE menu.flyway_schema_history;
DROP TABLE menu.categories;
DROP TABLE menu.ai_semantic_cache;
DROP TABLE kds.kds_tickets;
DROP TABLE kds.kds_ticket_items;
DROP TABLE kds.kds_ticket_item_options;
DROP TABLE kds.flyway_schema_history;
DROP TABLE ai.knowledge_base;
DROP TABLE ai.admin_semantic_cache;
DROP EXTENSION vector;
DROP SCHEMA orders;
DROP SCHEMA menu;
DROP SCHEMA kds;
DROP SCHEMA auth;
DROP SCHEMA ai;
--
-- Name: ai; Type: SCHEMA; Schema: -; Owner: fnb_user
--

CREATE SCHEMA ai;


ALTER SCHEMA ai OWNER TO fnb_user;

--
-- Name: auth; Type: SCHEMA; Schema: -; Owner: fnb_user
--




--
-- Name: kds; Type: SCHEMA; Schema: -; Owner: fnb_user
--

CREATE SCHEMA kds;


ALTER SCHEMA kds OWNER TO fnb_user;

--
-- Name: menu; Type: SCHEMA; Schema: -; Owner: fnb_user
--

CREATE SCHEMA menu;


ALTER SCHEMA menu OWNER TO fnb_user;

--
-- Name: orders; Type: SCHEMA; Schema: -; Owner: fnb_user
--

CREATE SCHEMA orders;


ALTER SCHEMA orders OWNER TO fnb_user;

--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;


--
-- Name: EXTENSION vector; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin_semantic_cache; Type: TABLE; Schema: ai; Owner: fnb_user
--

CREATE TABLE ai.admin_semantic_cache (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    question text NOT NULL,
    embedding public.vector(384),
    answer text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE ai.admin_semantic_cache OWNER TO fnb_user;

--
-- Name: knowledge_base; Type: TABLE; Schema: ai; Owner: fnb_user
--

CREATE TABLE ai.knowledge_base (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    category character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    content text NOT NULL,
    embedding public.vector(384)
);


ALTER TABLE ai.knowledge_base OWNER TO fnb_user;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: auth; Owner: fnb_user
--

    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);



--
-- Name: refresh_tokens; Type: TABLE; Schema: auth; Owner: fnb_user
--

    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    token character varying(500) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);



--
-- Name: users; Type: TABLE; Schema: auth; Owner: fnb_user
--

    id uuid DEFAULT gen_random_uuid() NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    full_name character varying(100),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    phone character varying(20),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'CASHIER'::character varying, 'KITCHEN'::character varying, 'SERVER'::character varying])::text[])))
);



--
-- Name: flyway_schema_history; Type: TABLE; Schema: kds; Owner: fnb_user
--

CREATE TABLE kds.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE kds.flyway_schema_history OWNER TO fnb_user;

--
-- Name: kds_ticket_item_options; Type: TABLE; Schema: kds; Owner: fnb_user
--

CREATE TABLE kds.kds_ticket_item_options (
    item_id uuid NOT NULL,
    option_name character varying(255)
);


ALTER TABLE kds.kds_ticket_item_options OWNER TO fnb_user;

--
-- Name: kds_ticket_items; Type: TABLE; Schema: kds; Owner: fnb_user
--

CREATE TABLE kds.kds_ticket_items (
    id uuid NOT NULL,
    kds_ticket_id uuid NOT NULL,
    menu_item_id uuid,
    item_name character varying(255),
    quantity integer,
    note character varying(255),
    station character varying(255),
    status character varying(255) DEFAULT 'PENDING'::character varying,
    prepared_by uuid,
    is_alert_sent boolean DEFAULT false,
    completed_at timestamp without time zone
);


ALTER TABLE kds.kds_ticket_items OWNER TO fnb_user;

--
-- Name: kds_tickets; Type: TABLE; Schema: kds; Owner: fnb_user
--

CREATE TABLE kds.kds_tickets (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    ticket_id uuid NOT NULL,
    table_number integer,
    session_token character varying(255),
    note character varying(255),
    status character varying(255) DEFAULT 'PENDING'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE kds.kds_tickets OWNER TO fnb_user;

--
-- Name: ai_semantic_cache; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.ai_semantic_cache (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    question text,
    embedding public.vector(384),
    answer text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE menu.ai_semantic_cache OWNER TO fnb_user;

--
-- Name: categories; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.categories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    image_url character varying(500),
    display_order integer DEFAULT 0,
    is_active boolean DEFAULT true
);


ALTER TABLE menu.categories OWNER TO fnb_user;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE menu.flyway_schema_history OWNER TO fnb_user;

--
-- Name: item_option_groups; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.item_option_groups (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    item_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    type character varying(10) NOT NULL,
    is_required boolean DEFAULT false,
    display_order integer DEFAULT 0,
    CONSTRAINT item_option_groups_type_check CHECK (((type)::text = ANY ((ARRAY['SINGLE'::character varying, 'MULTI'::character varying])::text[])))
);


ALTER TABLE menu.item_option_groups OWNER TO fnb_user;

--
-- Name: item_options; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.item_options (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    group_id uuid NOT NULL,
    name character varying(100) NOT NULL,
    extra_price numeric(12,2) DEFAULT 0,
    is_available boolean DEFAULT true
);


ALTER TABLE menu.item_options OWNER TO fnb_user;

--
-- Name: menu_items; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.menu_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    category_id uuid,
    name character varying(200) NOT NULL,
    description text,
    image_url character varying(500),
    base_price numeric(12,2) NOT NULL,
    station character varying(20) NOT NULL,
    is_available boolean DEFAULT true,
    is_featured boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    is_active boolean DEFAULT true NOT NULL,
    sale_price numeric(12,2),
    sale_start_at timestamp without time zone,
    sale_end_at timestamp without time zone,
    embedding public.vector(384),
    CONSTRAINT menu_items_base_price_check CHECK ((base_price >= (0)::numeric)),
    CONSTRAINT menu_items_station_check CHECK (((station)::text = ANY ((ARRAY['HOT'::character varying, 'COLD'::character varying, 'DRINK'::character varying])::text[])))
);


ALTER TABLE menu.menu_items OWNER TO fnb_user;

--
-- Name: COLUMN menu_items.sale_price; Type: COMMENT; Schema: menu; Owner: fnb_user
--

COMMENT ON COLUMN menu.menu_items.sale_price IS 'GiĂ¡ khuyáº¿n mĂ£i dĂ nh cho Flash Sale/Giáº£m giĂ¡ trá»±c tiáº¿p';


--
-- Name: promotion_bundle_items; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.promotion_bundle_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    promotion_id uuid NOT NULL,
    item_id uuid NOT NULL,
    quantity integer DEFAULT 1 NOT NULL,
    role character varying(10) NOT NULL,
    CONSTRAINT promotion_bundle_items_role_check CHECK (((role)::text = ANY ((ARRAY['BUY'::character varying, 'GET'::character varying])::text[])))
);


ALTER TABLE menu.promotion_bundle_items OWNER TO fnb_user;

--
-- Name: promotion_requirements; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.promotion_requirements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    promotion_id uuid NOT NULL,
    min_order_amount numeric(12,2) DEFAULT 0 NOT NULL,
    min_quantity integer DEFAULT 0 NOT NULL,
    member_level character varying(50)
);


ALTER TABLE menu.promotion_requirements OWNER TO fnb_user;

--
-- Name: promotion_schedules; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.promotion_schedules (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    promotion_id uuid NOT NULL,
    day_of_week integer NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    CONSTRAINT promotion_schedules_day_of_week_check CHECK (((day_of_week >= 0) AND (day_of_week <= 6)))
);


ALTER TABLE menu.promotion_schedules OWNER TO fnb_user;

--
-- Name: promotion_targets; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.promotion_targets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    promotion_id uuid NOT NULL,
    target_type character varying(20) NOT NULL,
    target_id uuid,
    CONSTRAINT promotion_targets_target_type_check CHECK (((target_type)::text = ANY ((ARRAY['ITEM'::character varying, 'CATEGORY'::character varying, 'GLOBAL'::character varying])::text[])))
);


ALTER TABLE menu.promotion_targets OWNER TO fnb_user;

--
-- Name: promotions; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.promotions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50),
    name character varying(200) NOT NULL,
    scope character varying(20) NOT NULL,
    trigger_type character varying(20) NOT NULL,
    discount_type character varying(20) NOT NULL,
    discount_value numeric(12,2),
    max_discount numeric(12,2),
    usage_limit integer,
    used_count integer DEFAULT 0 NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    start_at timestamp without time zone,
    end_at timestamp without time zone,
    is_stackable boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT promotions_discount_type_check CHECK (((discount_type)::text = ANY ((ARRAY['PERCENT'::character varying, 'FIX_AMOUNT'::character varying, 'FIX_PRICE'::character varying])::text[]))),
    CONSTRAINT promotions_scope_check CHECK (((scope)::text = ANY ((ARRAY['PRODUCT'::character varying, 'ORDER'::character varying, 'BUNDLE'::character varying])::text[]))),
    CONSTRAINT promotions_trigger_type_check CHECK (((trigger_type)::text = ANY ((ARRAY['AUTO'::character varying, 'COUPON'::character varying])::text[])))
);


ALTER TABLE menu.promotions OWNER TO fnb_user;

--
-- Name: restaurant_profile; Type: TABLE; Schema: menu; Owner: fnb_user
--

CREATE TABLE menu.restaurant_profile (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(255) DEFAULT 'NhĂ  HĂ ng ABC'::character varying NOT NULL,
    slogan character varying(512),
    logo_url text,
    banner_url text,
    address character varying(512),
    phone character varying(50),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE menu.restaurant_profile OWNER TO fnb_user;

--
-- Name: audit_logs; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.audit_logs (
    id uuid NOT NULL,
    action_name character varying(255) NOT NULL,
    user_id character varying(255),
    role character varying(255),
    target_id character varying(255),
    details text,
    ip_address character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE orders.audit_logs OWNER TO fnb_user;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE orders.flyway_schema_history OWNER TO fnb_user;

--
-- Name: order_item_options; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.order_item_options (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    ticket_item_id uuid,
    option_name character varying(100) NOT NULL,
    extra_price numeric(38,2) DEFAULT 0
);


ALTER TABLE orders.order_item_options OWNER TO fnb_user;

--
-- Name: order_ticket_items; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.order_ticket_items (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    ticket_id uuid,
    menu_item_id uuid NOT NULL,
    item_name character varying(200) NOT NULL,
    unit_price numeric(38,2) NOT NULL,
    quantity integer DEFAULT 1 NOT NULL,
    note character varying(255),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    station character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    served_by uuid,
    cancelled_by uuid,
    served_at timestamp without time zone,
    completed_at timestamp(6) without time zone,
    prepared_by uuid,
    is_alert_sent boolean DEFAULT false
);


ALTER TABLE orders.order_ticket_items OWNER TO fnb_user;

--
-- Name: order_tickets; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.order_tickets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid,
    seq_number integer NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    note character varying(255),
    created_at timestamp without time zone DEFAULT now(),
    created_by character varying(50)
);


ALTER TABLE orders.order_tickets OWNER TO fnb_user;

--
-- Name: orders; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.orders (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    session_id uuid,
    table_id uuid,
    source character varying(20) NOT NULL,
    order_type character varying(20) DEFAULT 'DINE_IN'::character varying,
    status character varying(30) DEFAULT 'OPEN'::character varying,
    subtotal numeric(38,2) DEFAULT 0,
    discount numeric(38,2) DEFAULT 0,
    tax numeric(38,2) DEFAULT 0,
    service_fee numeric(38,2) DEFAULT 0,
    total numeric(38,2) DEFAULT 0,
    promotion_id uuid,
    payment_method character varying(30),
    payment_detail jsonb,
    paid_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    discount_rate numeric(12,2),
    discount_type character varying(20),
    max_discount_value numeric(12,2),
    min_order_amount numeric(12,2),
    promotion_code character varying(50),
    payos_order_code bigint,
    is_stackable boolean,
    cashier_id uuid,
    cancelled_by uuid,
    cancel_reason character varying(255)
);


ALTER TABLE orders.orders OWNER TO fnb_user;

--
-- Name: COLUMN orders.payos_order_code; Type: COMMENT; Schema: orders; Owner: fnb_user
--

COMMENT ON COLUMN orders.orders.payos_order_code IS 'Ma tham chieu thanh toan tu he thong PayOS';


--
-- Name: staff_calls; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.staff_calls (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    session_id uuid,
    table_id uuid,
    message character varying(200),
    status character varying(20) DEFAULT 'PENDING'::character varying,
    called_at timestamp without time zone DEFAULT now(),
    resolved_at timestamp without time zone,
    created_at timestamp(6) without time zone,
    call_type character varying(50) NOT NULL,
    resolved_by uuid,
    accepted_by uuid,
    is_spillover_sent boolean DEFAULT false,
    accepted_at timestamp without time zone,
    CONSTRAINT staff_calls_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'RESOLVED'::character varying])::text[])))
);


ALTER TABLE orders.staff_calls OWNER TO fnb_user;

--
-- Name: table_info; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.table_info (
    id uuid NOT NULL,
    number integer
);


ALTER TABLE orders.table_info OWNER TO fnb_user;

--
-- Name: table_sessions; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.table_sessions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    table_id uuid,
    session_token character varying(100) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    opened_at timestamp without time zone DEFAULT now(),
    closed_at timestamp without time zone,
    expires_at timestamp without time zone NOT NULL
);


ALTER TABLE orders.table_sessions OWNER TO fnb_user;

--
-- Name: tables; Type: TABLE; Schema: orders; Owner: fnb_user
--

CREATE TABLE orders.tables (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    number integer NOT NULL,
    name character varying(50),
    qr_token character varying(100),
    qr_url character varying(500),
    status character varying(20) DEFAULT 'FREE'::character varying,
    capacity integer DEFAULT 4,
    is_active boolean DEFAULT true,
    zone character varying(50),
    parent_table_id uuid
);


ALTER TABLE orders.tables OWNER TO fnb_user;

--
-- Name: COLUMN tables.zone; Type: COMMENT; Schema: orders; Owner: fnb_user
--

COMMENT ON COLUMN orders.tables.zone IS 'Khu vá»±c bĂ n (Táº§ng 1, Ban cĂ´ng, VIP, ...)';


--
-- Data for Name: admin_semantic_cache; Type: TABLE DATA; Schema: ai; Owner: fnb_user
--



--
-- Data for Name: knowledge_base; Type: TABLE DATA; Schema: ai; Owner: fnb_user
--



--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: auth; Owner: fnb_user
--



--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: auth; Owner: fnb_user
--



--
-- Data for Name: users; Type: TABLE DATA; Schema: auth; Owner: fnb_user
--



--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: kds; Owner: fnb_user
--



--
-- Data for Name: kds_ticket_item_options; Type: TABLE DATA; Schema: kds; Owner: fnb_user
--



--
-- Data for Name: kds_ticket_items; Type: TABLE DATA; Schema: kds; Owner: fnb_user
--



--
-- Data for Name: kds_tickets; Type: TABLE DATA; Schema: kds; Owner: fnb_user
--



--
-- Data for Name: ai_semantic_cache; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: categories; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: item_option_groups; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: item_options; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: menu_items; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: promotion_bundle_items; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: promotion_requirements; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: promotion_schedules; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: promotion_targets; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: promotions; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: restaurant_profile; Type: TABLE DATA; Schema: menu; Owner: fnb_user
--



--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: order_item_options; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: order_ticket_items; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: order_tickets; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: orders; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: staff_calls; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: table_info; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: table_sessions; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Data for Name: tables; Type: TABLE DATA; Schema: orders; Owner: fnb_user
--



--
-- Name: admin_semantic_cache admin_semantic_cache_pkey; Type: CONSTRAINT; Schema: ai; Owner: fnb_user
--

ALTER TABLE ONLY ai.admin_semantic_cache
    ADD CONSTRAINT admin_semantic_cache_pkey PRIMARY KEY (id);


--
-- Name: knowledge_base knowledge_base_pkey; Type: CONSTRAINT; Schema: ai; Owner: fnb_user
--

ALTER TABLE ONLY ai.knowledge_base
    ADD CONSTRAINT knowledge_base_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: auth; Owner: fnb_user
--

    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: auth; Owner: fnb_user
--

    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_key; Type: CONSTRAINT; Schema: auth; Owner: fnb_user
--

    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: auth; Owner: fnb_user
--

    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: auth; Owner: fnb_user
--

    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: kds; Owner: fnb_user
--

ALTER TABLE ONLY kds.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: kds_ticket_items kds_ticket_items_pkey; Type: CONSTRAINT; Schema: kds; Owner: fnb_user
--

ALTER TABLE ONLY kds.kds_ticket_items
    ADD CONSTRAINT kds_ticket_items_pkey PRIMARY KEY (id);


--
-- Name: kds_tickets kds_tickets_pkey; Type: CONSTRAINT; Schema: kds; Owner: fnb_user
--

ALTER TABLE ONLY kds.kds_tickets
    ADD CONSTRAINT kds_tickets_pkey PRIMARY KEY (id);


--
-- Name: ai_semantic_cache ai_semantic_cache_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.ai_semantic_cache
    ADD CONSTRAINT ai_semantic_cache_pkey PRIMARY KEY (id);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: item_option_groups item_option_groups_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.item_option_groups
    ADD CONSTRAINT item_option_groups_pkey PRIMARY KEY (id);


--
-- Name: item_options item_options_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.item_options
    ADD CONSTRAINT item_options_pkey PRIMARY KEY (id);


--
-- Name: menu_items menu_items_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.menu_items
    ADD CONSTRAINT menu_items_pkey PRIMARY KEY (id);


--
-- Name: promotion_bundle_items promotion_bundle_items_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_bundle_items
    ADD CONSTRAINT promotion_bundle_items_pkey PRIMARY KEY (id);


--
-- Name: promotion_requirements promotion_requirements_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_requirements
    ADD CONSTRAINT promotion_requirements_pkey PRIMARY KEY (id);


--
-- Name: promotion_schedules promotion_schedules_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_schedules
    ADD CONSTRAINT promotion_schedules_pkey PRIMARY KEY (id);


--
-- Name: promotion_targets promotion_targets_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_targets
    ADD CONSTRAINT promotion_targets_pkey PRIMARY KEY (id);


--
-- Name: promotions promotions_code_key; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotions
    ADD CONSTRAINT promotions_code_key UNIQUE (code);


--
-- Name: promotions promotions_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotions
    ADD CONSTRAINT promotions_pkey PRIMARY KEY (id);


--
-- Name: restaurant_profile restaurant_profile_pkey; Type: CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.restaurant_profile
    ADD CONSTRAINT restaurant_profile_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: order_item_options order_item_options_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_item_options
    ADD CONSTRAINT order_item_options_pkey PRIMARY KEY (id);


--
-- Name: order_ticket_items order_ticket_items_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_ticket_items
    ADD CONSTRAINT order_ticket_items_pkey PRIMARY KEY (id);


--
-- Name: order_tickets order_tickets_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_tickets
    ADD CONSTRAINT order_tickets_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: staff_calls staff_calls_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.staff_calls
    ADD CONSTRAINT staff_calls_pkey PRIMARY KEY (id);


--
-- Name: table_info table_info_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.table_info
    ADD CONSTRAINT table_info_pkey PRIMARY KEY (id);


--
-- Name: table_sessions table_sessions_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.table_sessions
    ADD CONSTRAINT table_sessions_pkey PRIMARY KEY (id);


--
-- Name: table_sessions table_sessions_session_token_key; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.table_sessions
    ADD CONSTRAINT table_sessions_session_token_key UNIQUE (session_token);


--
-- Name: tables tables_number_key; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.tables
    ADD CONSTRAINT tables_number_key UNIQUE (number);


--
-- Name: tables tables_pkey; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.tables
    ADD CONSTRAINT tables_pkey PRIMARY KEY (id);


--
-- Name: tables tables_qr_token_key; Type: CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.tables
    ADD CONSTRAINT tables_qr_token_key UNIQUE (qr_token);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: auth; Owner: fnb_user
--



--
-- Name: idx_refresh_tokens_token; Type: INDEX; Schema: auth; Owner: fnb_user
--



--
-- Name: idx_refresh_tokens_user_id; Type: INDEX; Schema: auth; Owner: fnb_user
--



--
-- Name: idx_users_username; Type: INDEX; Schema: auth; Owner: fnb_user
--



--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: kds; Owner: fnb_user
--

CREATE INDEX flyway_schema_history_s_idx ON kds.flyway_schema_history USING btree (success);


--
-- Name: idx_kds_items_prepared_by; Type: INDEX; Schema: kds; Owner: fnb_user
--

CREATE INDEX idx_kds_items_prepared_by ON kds.kds_ticket_items USING btree (prepared_by) WHERE (prepared_by IS NOT NULL);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX flyway_schema_history_s_idx ON menu.flyway_schema_history USING btree (success);


--
-- Name: idx_categories_active; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_categories_active ON menu.categories USING btree (is_active);


--
-- Name: idx_item_options_group; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_item_options_group ON menu.item_options USING btree (group_id);


--
-- Name: idx_menu_items_available; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_menu_items_available ON menu.menu_items USING btree (is_available, is_featured);


--
-- Name: idx_menu_items_category; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_menu_items_category ON menu.menu_items USING btree (category_id);


--
-- Name: idx_option_groups_item; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_option_groups_item ON menu.item_option_groups USING btree (item_id);


--
-- Name: idx_promo_bundle_item; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_promo_bundle_item ON menu.promotion_bundle_items USING btree (item_id);


--
-- Name: idx_promo_targets_type; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_promo_targets_type ON menu.promotion_targets USING btree (target_type, target_id);


--
-- Name: idx_promotions_active; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_promotions_active ON menu.promotions USING btree (is_active, start_at, end_at);


--
-- Name: idx_promotions_code; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_promotions_code ON menu.promotions USING btree (code) WHERE (code IS NOT NULL);


--
-- Name: idx_promotions_scope; Type: INDEX; Schema: menu; Owner: fnb_user
--

CREATE INDEX idx_promotions_scope ON menu.promotions USING btree (scope, is_active);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX flyway_schema_history_s_idx ON orders.flyway_schema_history USING btree (success);


--
-- Name: idx_audit_logs_action_name; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_audit_logs_action_name ON orders.audit_logs USING btree (action_name);


--
-- Name: idx_audit_logs_target_id; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_audit_logs_target_id ON orders.audit_logs USING btree (target_id);


--
-- Name: idx_audit_logs_user_id; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_audit_logs_user_id ON orders.audit_logs USING btree (user_id);


--
-- Name: idx_order_items_served_by; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_order_items_served_by ON orders.order_ticket_items USING btree (served_by) WHERE (served_by IS NOT NULL);


--
-- Name: idx_orders_cashier; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_orders_cashier ON orders.orders USING btree (cashier_id) WHERE (cashier_id IS NOT NULL);


--
-- Name: idx_staff_calls_pending; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_staff_calls_pending ON orders.staff_calls USING btree (status, created_at) WHERE ((status)::text = 'PENDING'::text);


--
-- Name: idx_staff_calls_resolved_by; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_staff_calls_resolved_by ON orders.staff_calls USING btree (resolved_by) WHERE (resolved_by IS NOT NULL);


--
-- Name: idx_ticket_items_status_done; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE INDEX idx_ticket_items_status_done ON orders.order_ticket_items USING btree (status, created_at) WHERE ((status)::text = 'DONE'::text);


--
-- Name: unq_active_order; Type: INDEX; Schema: orders; Owner: fnb_user
--

CREATE UNIQUE INDEX unq_active_order ON orders.orders USING btree (session_id) WHERE ((status)::text = ANY ((ARRAY['OPEN'::character varying, 'PAYMENT_REQUESTED'::character varying])::text[]));


--
-- Name: refresh_tokens refresh_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: auth; Owner: fnb_user
--



--
-- Name: kds_ticket_items fk_kds_ticket; Type: FK CONSTRAINT; Schema: kds; Owner: fnb_user
--

ALTER TABLE ONLY kds.kds_ticket_items
    ADD CONSTRAINT fk_kds_ticket FOREIGN KEY (kds_ticket_id) REFERENCES kds.kds_tickets(id) ON DELETE CASCADE;


--
-- Name: kds_ticket_item_options fk_kds_ticket_item_option; Type: FK CONSTRAINT; Schema: kds; Owner: fnb_user
--

ALTER TABLE ONLY kds.kds_ticket_item_options
    ADD CONSTRAINT fk_kds_ticket_item_option FOREIGN KEY (item_id) REFERENCES kds.kds_ticket_items(id) ON DELETE CASCADE;


--
-- Name: kds_ticket_items kds_ticket_items_prepared_by_fkey; Type: FK CONSTRAINT; Schema: kds; Owner: fnb_user
--

ALTER TABLE ONLY kds.kds_ticket_items


--
-- Name: item_option_groups item_option_groups_item_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.item_option_groups
    ADD CONSTRAINT item_option_groups_item_id_fkey FOREIGN KEY (item_id) REFERENCES menu.menu_items(id) ON DELETE CASCADE;


--
-- Name: item_options item_options_group_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.item_options
    ADD CONSTRAINT item_options_group_id_fkey FOREIGN KEY (group_id) REFERENCES menu.item_option_groups(id) ON DELETE CASCADE;


--
-- Name: menu_items menu_items_category_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.menu_items
    ADD CONSTRAINT menu_items_category_id_fkey FOREIGN KEY (category_id) REFERENCES menu.categories(id) ON DELETE SET NULL;


--
-- Name: promotion_bundle_items promotion_bundle_items_item_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_bundle_items
    ADD CONSTRAINT promotion_bundle_items_item_id_fkey FOREIGN KEY (item_id) REFERENCES menu.menu_items(id) ON DELETE CASCADE;


--
-- Name: promotion_bundle_items promotion_bundle_items_promotion_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_bundle_items
    ADD CONSTRAINT promotion_bundle_items_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES menu.promotions(id) ON DELETE CASCADE;


--
-- Name: promotion_requirements promotion_requirements_promotion_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_requirements
    ADD CONSTRAINT promotion_requirements_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES menu.promotions(id) ON DELETE CASCADE;


--
-- Name: promotion_schedules promotion_schedules_promotion_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_schedules
    ADD CONSTRAINT promotion_schedules_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES menu.promotions(id) ON DELETE CASCADE;


--
-- Name: promotion_targets promotion_targets_promotion_id_fkey; Type: FK CONSTRAINT; Schema: menu; Owner: fnb_user
--

ALTER TABLE ONLY menu.promotion_targets
    ADD CONSTRAINT promotion_targets_promotion_id_fkey FOREIGN KEY (promotion_id) REFERENCES menu.promotions(id) ON DELETE CASCADE;


--
-- Name: tables fk_parent_table; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.tables
    ADD CONSTRAINT fk_parent_table FOREIGN KEY (parent_table_id) REFERENCES orders.tables(id);


--
-- Name: order_item_options order_item_options_ticket_item_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_item_options
    ADD CONSTRAINT order_item_options_ticket_item_id_fkey FOREIGN KEY (ticket_item_id) REFERENCES orders.order_ticket_items(id);


--
-- Name: order_ticket_items order_ticket_items_cancelled_by_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_ticket_items


--
-- Name: order_ticket_items order_ticket_items_served_by_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_ticket_items


--
-- Name: order_ticket_items order_ticket_items_ticket_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_ticket_items
    ADD CONSTRAINT order_ticket_items_ticket_id_fkey FOREIGN KEY (ticket_id) REFERENCES orders.order_tickets(id);


--
-- Name: order_tickets order_tickets_order_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.order_tickets
    ADD CONSTRAINT order_tickets_order_id_fkey FOREIGN KEY (order_id) REFERENCES orders.orders(id);


--
-- Name: orders orders_cancelled_by_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.orders


--
-- Name: orders orders_cashier_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.orders


--
-- Name: orders orders_session_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.orders
    ADD CONSTRAINT orders_session_id_fkey FOREIGN KEY (session_id) REFERENCES orders.table_sessions(id);


--
-- Name: orders orders_table_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.orders
    ADD CONSTRAINT orders_table_id_fkey FOREIGN KEY (table_id) REFERENCES orders.tables(id);


--
-- Name: staff_calls staff_calls_accepted_by_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.staff_calls


--
-- Name: staff_calls staff_calls_resolved_by_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.staff_calls


--
-- Name: staff_calls staff_calls_session_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.staff_calls
    ADD CONSTRAINT staff_calls_session_id_fkey FOREIGN KEY (session_id) REFERENCES orders.table_sessions(id);


--
-- Name: staff_calls staff_calls_table_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.staff_calls
    ADD CONSTRAINT staff_calls_table_id_fkey FOREIGN KEY (table_id) REFERENCES orders.tables(id);


--
-- Name: table_sessions table_sessions_table_id_fkey; Type: FK CONSTRAINT; Schema: orders; Owner: fnb_user
--

ALTER TABLE ONLY orders.table_sessions
    ADD CONSTRAINT table_sessions_table_id_fkey FOREIGN KEY (table_id) REFERENCES orders.tables(id);


--
-- PostgreSQL database dump complete
--

\unrestrict DTT9chsWrQimdQNP7nYjvllDNzhOAJg2aCqipPTPQRjGXB9OiK6KeK1MPPfzpS6

