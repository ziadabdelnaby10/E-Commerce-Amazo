insert into users (email
                 , password_hash
                 , first_name
                 , last_name
                 , phone_number
                 , is_active
                 , is_email_verified
                 , email_verified_at
                 , last_login_at)
values ('admin@gmail.com',
        '$2a$12$sq6BRNII/uQ9SIHtecguPe1.cwD9r/46Q23kj2a42Bjody1FowHBu',
        'Admin',
        'User',
        '+1234567890',
        true,
        true,
        CURRENT_TIMESTAMP
           , CURRENT_TIMESTAMP);


INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT
    u.id AS user_id,
    r.id AS role_id,
    u.id AS assigned_by -- Setting the admin as the one who assigned it to themselves
FROM
    users u,
    roles r
WHERE
    u.email = 'admin@gmail.com'
  AND r.name = 'ROLE_ADMIN';
