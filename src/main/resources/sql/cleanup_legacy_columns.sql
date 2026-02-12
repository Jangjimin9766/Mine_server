-- Remove deprecated columns from magazine_section table
ALTER TABLE magazine_section DROP COLUMN content;
ALTER TABLE magazine_section DROP COLUMN image_url;
ALTER TABLE magazine_section DROP COLUMN caption;
