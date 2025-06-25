ALTER TABLE public.output
    ADD COLUMN derived_from uuid;

ALTER TABLE public.output
    ADD CONSTRAINT fk_output_derived_from_input
    FOREIGN KEY (derived_from) REFERENCES public.input (id);
