-- Execute primeiro no MySQL Workbench ou cliente mysql.
-- Sem IF NOT EXISTS: se o banco ja existir, pare e escolha outro nome.
-- Este arquivo nao apaga nem substitui nenhum banco existente.
CREATE DATABASE bee_good_food_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE bee_good_food_demo;
SET time_zone = '+00:00';
-- Depois, neste mesmo schema, execute os arquivos nesta ordem:
-- 1. migrations/V1__criar_schema_inicial.sql
-- 2. migrations/V2__cadastrar_papeis.sql
-- 3. seeds/demonstracao.sql
-- 4. queries/consultas-estudo.sql
