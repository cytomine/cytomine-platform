import * as utils from './utils.js';
import {AnnotationTerm} from '@/index.js';

describe('AnnotationTerm', () => {
  let userannotation;
  let term;

  let annotationTerm = null;

  beforeAll(async () => {
    await utils.connect();
    ({id: userannotation} = await utils.getAnnotation());
    ({id: term} = await utils.getTerm());
  });

  describe('Create', () => {
    it('Create', async () => {
      annotationTerm = new AnnotationTerm({userannotation, term});
      annotationTerm = await annotationTerm.save();
      expect(annotationTerm).toBeInstanceOf(AnnotationTerm);
    });

    it('Create and clear previous', async () => {
      annotationTerm = new AnnotationTerm({userannotation, term});
      annotationTerm = await annotationTerm.saveAndClearPrevious();
      expect(annotationTerm).toBeInstanceOf(AnnotationTerm);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedAnnotationTerm = await AnnotationTerm.fetch(userannotation, term);
      expect(fetchedAnnotationTerm).toBeInstanceOf(AnnotationTerm);
      expect(fetchedAnnotationTerm).toEqual(annotationTerm);
    });

    it('Fetch with instance method', async () => {
      let fetchedAnnotationTerm = await new AnnotationTerm({userannotation, term}).fetch();
      expect(fetchedAnnotationTerm).toBeInstanceOf(AnnotationTerm);
      expect(fetchedAnnotationTerm).toEqual(annotationTerm);
    });

    it('Fetch with wrong ID', async () => {
      await expect(AnnotationTerm.fetch(0)).rejects.toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await AnnotationTerm.delete(userannotation, term);
    });

    it('Fetch deleted', async () => {
      await expect(AnnotationTerm.fetch(userannotation, term)).rejects.toThrow();
    });
  });
});
