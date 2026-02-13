-- ============================================
-- User Service - Add Permission Translations
-- Version: 9
-- Description: Ajouter les colonnes de traduction pour les descriptions
--              des permissions (EN et AR) pour support multilingue
-- ============================================

-- Ajouter les colonnes pour les traductions
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS description_en VARCHAR(255);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS description_ar VARCHAR(255);

-- Mettre à jour les traductions pour toutes les permissions existantes

-- Dashboard
UPDATE permissions SET 
    description_en = 'Access to the main dashboard',
    description_ar = 'الوصول إلى لوحة التحكم الرئيسية'
WHERE module = 'dashboard';

-- Users
UPDATE permissions SET 
    description_en = 'Complete management of customers and couriers',
    description_ar = 'الإدارة الكاملة للعملاء والسعاة'
WHERE module = 'users';

-- Admins
UPDATE permissions SET 
    description_en = 'Management of administrators and roles',
    description_ar = 'إدارة المسؤولين والأدوار'
WHERE module = 'admins';

-- Orders
UPDATE permissions SET 
    description_en = 'Complete order management',
    description_ar = 'إدارة الطلبات الكاملة'
WHERE module = 'orders';

-- Partners
UPDATE permissions SET 
    description_en = 'Management of restaurants and partners',
    description_ar = 'إدارة المطاعم والشركاء'
WHERE module = 'partners';

-- Delivery
UPDATE permissions SET 
    description_en = 'Delivery management and tracking',
    description_ar = 'إدارة وتتبع التوصيلات'
WHERE module = 'delivery';

-- Payments
UPDATE permissions SET 
    description_en = 'Payment and transaction management',
    description_ar = 'إدارة المدفوعات والمعاملات'
WHERE module = 'payments';

-- Promotions
UPDATE permissions SET 
    description_en = 'Management of promotions and promo codes',
    description_ar = 'إدارة العروض والرموز الترويجية'
WHERE module = 'promotions';

-- Reviews
UPDATE permissions SET 
    description_en = 'Moderation of reviews and ratings',
    description_ar = 'مراجعة التقييمات والتعليقات'
WHERE module = 'reviews';

-- Analytics
UPDATE permissions SET 
    description_en = 'Access to statistics and reports',
    description_ar = 'الوصول إلى الإحصائيات والتقارير'
WHERE module = 'analytics';

-- Notifications
UPDATE permissions SET 
    description_en = 'Sending and managing notifications',
    description_ar = 'إرسال وإدارة الإشعارات'
WHERE module = 'notifications';

-- Settings
UPDATE permissions SET 
    description_en = 'Complete system configuration',
    description_ar = 'تكوين النظام الكامل'
WHERE module = 'settings';

-- Support
UPDATE permissions SET 
    description_en = 'Customer support ticket management',
    description_ar = 'إدارة تذاكر دعم العملاء'
WHERE module = 'support';

-- Zones
UPDATE permissions SET 
    description_en = 'Delivery zone management',
    description_ar = 'إدارة مناطق التوصيل'
WHERE module = 'zones';

-- Monitoring
UPDATE permissions SET 
    description_en = 'System monitoring and status',
    description_ar = 'مراقبة النظام والحالة'
WHERE module = 'monitoring';
