import Collection from './collection.js';
import Discipline from '../models/discipline.js';

export default class DisciplineCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Discipline;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}
