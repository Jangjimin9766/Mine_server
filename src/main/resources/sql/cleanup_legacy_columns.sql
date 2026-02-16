-- Remove deprecated columns from magazine_sections table
ALTER TABLE magazine_sections DROP COLUMN content;
ALTER TABLE magazine_sections DROP COLUMN image_url;
ALTER TABLE magazine_sections DROP COLUMN caption;
