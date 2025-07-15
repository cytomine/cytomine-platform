import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';

import CytomineProperties from '@/components/property/CytomineProperties';
import {Property} from 'cytomine-client';

jest.mock('cytomine-client', () => {
  return {
    Property: jest.fn().mockImplementation(() => {
      return {
        id: Date.now(),
        key: '',
        value: '',
        isNew: jest.fn().mockReturnValue(true),
        save: jest.fn().mockResolvedValue(true),
      };
    }),
    PropertyCollection: {
      fetchAll: jest.fn().mockResolvedValue({
        array: [
          {id: 1, key: 'Property 1', value: 'Value 1'},
        ],
      }),
    },
  };
});

describe('CytomineProperties.vue', () => {
  const mocks = {
    $notify: jest.fn(),
    $t: message => message,
  };

  let wrapper;
  let localVue;

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = shallowMount(CytomineProperties, {
      localVue,
      mocks: mocks,
      propsData: {
        canEdit: true,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.find('.properties-wrapper').exists()).toBe(true);
  });

  it('The component should render the initial properties correctly', () => {
    expect(wrapper.text()).toContain('Property 1');
    expect(wrapper.text()).toContain('Value 1');
  });

  it('The component should contain the correct initial data', () => {
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.error).toBe(false);
    expect(wrapper.vm.properties.length).toBe(1);
    expect(wrapper.vm.editedProperties.length).toBe(0);
    expect(wrapper.vm.newPropKey).toBe('');
    expect(wrapper.vm.newPropValue).toBe('');
    expect(wrapper.vm.showNewPropForm).toBe(false);
  });

  it('Clicking on add button should add a new property correctly', async () => {
    const property = new Property({object: {}});
    property.id = 2;
    property.key = 'New Property';
    property.value = 'New Value';

    await wrapper.setData({
      editedProperties: [property]
    });

    wrapper.find('button.add-prop').trigger('click');

    expect(wrapper.vm.editedProperties.length).toBe(2);
  });

  it('Clicking on delete button should delete a property correctly', async () => {
    Property.delete = jest.fn().mockResolvedValue(true);

    expect(wrapper.vm.properties.length).toBe(1);

    const deleteButton = wrapper.find('button.delete.is-small');
    await deleteButton.trigger('click');
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted()).toHaveProperty('update');
    expect(wrapper.vm.properties.length).toBe(0);
  });

  it('Clicking on save button should save the modification correctly', async () => {
    const property = new Property({object: {}});
    property.key = 'New Property';
    property.value = 'New Value';

    wrapper.setData({
      editedProperties: [property]
    });

    await wrapper.vm.saveProp(0);

    expect(wrapper.emitted()).toHaveProperty('update');
  });

  it('Cancel an edition should handle property cancel correctly', async () => {
    const property = new Property({object: {}});
    property.key = 'New Property';
    property.value = 'New Value';

    wrapper.setData({
      editedProperties: [property]
    });

    await wrapper.vm.cancelPropEdition(0);

    expect(wrapper.vm.editedProperties.length).toBe(0);
  });
});
