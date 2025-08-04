INSERT INTO client (name, email, phone, cpf, points) VALUES ('John Doe', 'john.doe@example.com', '889988889988', '123.456.789-00', 0);
INSERT INTO client (name, email, phone, cpf, points) VALUES ('Jane Smith', 'jane.smith@example.com', '98765432100', '987.654.321-00', 0);

INSERT INTO establishment (name, email, phone, value_per_point, cnpj) VALUES ('Supermarket A', 'supermarketA@mail.com', '123-456-7890', 10, '11.111.111/0001-11');
INSERT INTO establishment (name, email, phone, value_per_point, cnpj) VALUES ('Restaurant B', 'restaurantB@mail.com', '321-345-8989', 50, '22.222.222/0001-22');

INSERT INTO purchase (client_id, establishment_id, amount) VALUES (1, 1, 150.75);
INSERT INTO purchase (client_id, establishment_id, amount) VALUES (2, 2, 85.50);
