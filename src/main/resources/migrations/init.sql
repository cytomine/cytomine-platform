--liquibase formatted sql

--changeset siddig.hamed:2 labels:initial-schema context:provisioning
--comment: this is the initial schema


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


CREATE TABLE public.author (
                               id uuid NOT NULL,
                               created_date timestamp(6) without time zone,
                               last_modified_date timestamp(6) without time zone,
                               email character varying(255),
                               first_name character varying(255),
                               is_contact boolean NOT NULL,
                               last_name character varying(255),
                               organization character varying(255)
);


ALTER TABLE public.author OWNER TO appengine;


CREATE TABLE public.input (
                              id uuid NOT NULL,
                              created_date timestamp(6) without time zone,
                              last_modified_date timestamp(6) without time zone,
                              default_value integer NOT NULL,
                              description character varying(255),
                              display_name character varying(255),
                              name character varying(255),
                              optional boolean NOT NULL,
                              type_identifier uuid
);


ALTER TABLE public.input OWNER TO appengine;


CREATE TABLE public.integer_provision (
                                          id uuid NOT NULL,
                                          created_date timestamp(6) without time zone,
                                          last_modified_date timestamp(6) without time zone,
                                          parameter_name character varying(255),
                                          run_id uuid,
                                          value integer NOT NULL
);


ALTER TABLE public.integer_provision OWNER TO appengine;

CREATE TABLE public.integer_type (
                                     geq integer NOT NULL,
                                     gt integer NOT NULL,
                                     id character varying(255),
                                     leq integer NOT NULL,
                                     lt integer NOT NULL,
                                     identifier uuid NOT NULL
);


ALTER TABLE public.integer_type OWNER TO appengine;


CREATE TABLE public.output (
                               id uuid NOT NULL,
                               created_date timestamp(6) without time zone,
                               last_modified_date timestamp(6) without time zone,
                               default_value integer NOT NULL,
                               description character varying(255),
                               display_name character varying(255),
                               name character varying(255),
                               optional boolean NOT NULL,
                               type_identifier uuid
);


ALTER TABLE public.output OWNER TO appengine;


CREATE TABLE public.run (
                            id uuid NOT NULL,
                            created_date timestamp(6) without time zone,
                            last_modified_date timestamp(6) without time zone,
                            state smallint,
                            task_identifier uuid,
                            CONSTRAINT run_state_check CHECK (((state >= 0) AND (state <= 8)))
);


ALTER TABLE public.run OWNER TO appengine;

CREATE TABLE public.run_provisions (
                                       run_id uuid NOT NULL,
                                       provisions_id uuid NOT NULL
);


ALTER TABLE public.run_provisions OWNER TO appengine;

CREATE TABLE public.task (
                             identifier uuid NOT NULL,
                             created_date timestamp(6) without time zone,
                             last_modified_date timestamp(6) without time zone,
                             description character varying(255),
                             descriptor_file character varying(255),
                             name character varying(255),
                             name_short character varying(255),
                             namespace character varying(255),
                             storage_reference character varying(255),
                             version character varying(255)
);


ALTER TABLE public.task OWNER TO appengine;

CREATE TABLE public.task_authors (
                                     task_identifier uuid NOT NULL,
                                     authors_id uuid NOT NULL
);


ALTER TABLE public.task_authors OWNER TO appengine;

CREATE TABLE public.task_inputs (
                                    task_identifier uuid NOT NULL,
                                    inputs_id uuid NOT NULL
);


ALTER TABLE public.task_inputs OWNER TO appengine;


CREATE TABLE public.task_outputs (
                                     task_identifier uuid NOT NULL,
                                     outputs_id uuid NOT NULL
);


ALTER TABLE public.task_outputs OWNER TO appengine;

CREATE TABLE public.task_runs (
                                  task_identifier uuid NOT NULL,
                                  runs_id uuid NOT NULL
);


ALTER TABLE public.task_runs OWNER TO appengine;

CREATE TABLE public.type (
                             identifier uuid NOT NULL,
                             created_date timestamp(6) without time zone,
                             last_modified_date timestamp(6) without time zone
);


ALTER TABLE public.type OWNER TO appengine;

CREATE TABLE public.type_constraints (
                                         type_identifier uuid NOT NULL,
                                         constraints character varying(255)
);


ALTER TABLE public.type_constraints OWNER TO appengine;

ALTER TABLE ONLY public.author
    ADD CONSTRAINT author_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.input
    ADD CONSTRAINT input_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.integer_provision
    ADD CONSTRAINT integer_provision_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.integer_type
    ADD CONSTRAINT integer_type_pkey PRIMARY KEY (identifier);


ALTER TABLE ONLY public.output
    ADD CONSTRAINT output_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.run
    ADD CONSTRAINT run_pkey PRIMARY KEY (id);


ALTER TABLE ONLY public.run_provisions
    ADD CONSTRAINT run_provisions_pkey PRIMARY KEY (run_id, provisions_id);


ALTER TABLE ONLY public.task_authors
    ADD CONSTRAINT task_authors_pkey PRIMARY KEY (task_identifier, authors_id);


ALTER TABLE ONLY public.task_inputs
    ADD CONSTRAINT task_inputs_pkey PRIMARY KEY (task_identifier, inputs_id);


ALTER TABLE ONLY public.task_outputs
    ADD CONSTRAINT task_outputs_pkey PRIMARY KEY (task_identifier, outputs_id);

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_pkey PRIMARY KEY (identifier);


ALTER TABLE ONLY public.type
    ADD CONSTRAINT type_pkey PRIMARY KEY (identifier);


ALTER TABLE ONLY public.task_inputs
    ADD CONSTRAINT uk_1dfeiu4m4054njuht8u7b7lj UNIQUE (inputs_id);

ALTER TABLE ONLY public.task_runs
    ADD CONSTRAINT uk_23w7mixnpxqw47l2qc4m41pql UNIQUE (runs_id);


ALTER TABLE ONLY public.input
    ADD CONSTRAINT uk_73n9jegwfnc15i58umluy6so1 UNIQUE (type_identifier);


ALTER TABLE ONLY public.output
    ADD CONSTRAINT uk_gv70k85aesjl71vyubmw886ba UNIQUE (type_identifier);


ALTER TABLE ONLY public.run_provisions
    ADD CONSTRAINT uk_lctdhpdjye2yx2mmbx9lhprgh UNIQUE (provisions_id);

ALTER TABLE ONLY public.task_authors
    ADD CONSTRAINT uk_lhe0h4r63ip5yms54ctegedyd UNIQUE (authors_id);

ALTER TABLE ONLY public.task_outputs
    ADD CONSTRAINT uk_noqy3sk6x7cb568oppwr3ir4g UNIQUE (outputs_id);


ALTER TABLE ONLY public.task
    ADD CONSTRAINT ukaooh6s7q1faei0rr9eglv1g6k UNIQUE (namespace, version);


ALTER TABLE ONLY public.type_constraints
    ADD CONSTRAINT fk27l4vmr46umi6x5otq8m416ll FOREIGN KEY (type_identifier) REFERENCES public.type(identifier);


ALTER TABLE ONLY public.output
    ADD CONSTRAINT fk2cdqftrik9h0jnnl2uudsf37 FOREIGN KEY (type_identifier) REFERENCES public.type(identifier);


ALTER TABLE ONLY public.run
    ADD CONSTRAINT fk3vvax3dt2uy6n0xxwsqwpe5vx FOREIGN KEY (task_identifier) REFERENCES public.task(identifier) ON UPDATE CASCADE ON DELETE CASCADE;


ALTER TABLE ONLY public.run_provisions
    ADD CONSTRAINT fkalebdf7rgjwqxhheamg4cxyxy FOREIGN KEY (provisions_id) REFERENCES public.integer_provision(id) ON UPDATE CASCADE ON DELETE CASCADE;


ALTER TABLE ONLY public.run_provisions
    ADD CONSTRAINT fkbbqg43dri8fvoo1gtkvbea6us FOREIGN KEY (run_id) REFERENCES public.run(id) ON UPDATE CASCADE ON DELETE CASCADE;


ALTER TABLE ONLY public.task_outputs
    ADD CONSTRAINT fkfls5pwgnibb9m8gswabqsdipi FOREIGN KEY (task_identifier) REFERENCES public.task(identifier);

ALTER TABLE ONLY public.task_authors
    ADD CONSTRAINT fkgh9w5wsdjm0o2ql35aoqsv0u7 FOREIGN KEY (task_identifier) REFERENCES public.task(identifier);


ALTER TABLE ONLY public.integer_type
    ADD CONSTRAINT fkgrv93t6py9vlnl4hkker8bnss FOREIGN KEY (identifier) REFERENCES public.type(identifier);


ALTER TABLE ONLY public.task_outputs
    ADD CONSTRAINT fki11aaifgjr6xwd04crukh1uep FOREIGN KEY (outputs_id) REFERENCES public.output(id);


ALTER TABLE ONLY public.task_runs
    ADD CONSTRAINT fkkxl0h2di2na91rnjrxygxdjj2 FOREIGN KEY (runs_id) REFERENCES public.run(id);


ALTER TABLE ONLY public.task_authors
    ADD CONSTRAINT fkmg1yvs01reo9scucsv7gcse5e FOREIGN KEY (authors_id) REFERENCES public.author(id);


ALTER TABLE ONLY public.task_runs
    ADD CONSTRAINT fkmnluw29uq4or5xp8t0txxxste FOREIGN KEY (task_identifier) REFERENCES public.task(identifier);


ALTER TABLE ONLY public.task_inputs
    ADD CONSTRAINT fkqiemwv7f0hxltnprnv6ns716d FOREIGN KEY (task_identifier) REFERENCES public.task(identifier);

ALTER TABLE ONLY public.task_inputs
    ADD CONSTRAINT fkrhs2ycrec4uwfcji7cy2m4cmr FOREIGN KEY (inputs_id) REFERENCES public.input(id);


ALTER TABLE ONLY public.input
    ADD CONSTRAINT fksdprr90c856nlb1jbqvutlrua FOREIGN KEY (type_identifier) REFERENCES public.type(identifier);

alter table public.run
    drop constraint fk3vvax3dt2uy6n0xxwsqwpe5vx;

alter table public.run
    add constraint fk3vvax3dt2uy6n0xxwsqwpe5vx
        foreign key (task_identifier) references public.task
            on update cascade on delete cascade;




