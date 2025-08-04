-- Create Client table
CREATE TABLE client (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    cpf VARCHAR(255),
    points INT,
    PRIMARY KEY (id),
    UNIQUE (cpf)
);

-- Create Establishment table
CREATE TABLE establishment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    value_per_point INT,
    cnpj VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE (cnpj)
);

-- Create Purchase table
CREATE TABLE purchase (
    purchase_id BIGINT NOT NULL AUTO_INCREMENT,
    amount DECIMAL(19,2),
    client_id BIGINT,
    establishment_id BIGINT,
    PRIMARY KEY (purchase_id),
    FOREIGN KEY (client_id) REFERENCES client(id),
    FOREIGN KEY (establishment_id) REFERENCES establishment(id)
);
