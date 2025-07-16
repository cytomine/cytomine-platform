import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';

import attachedFiles from '@/components/attached-file/AttachedFiles';
import {AttachedFileCollection} from 'cytomine-client';

jest.mock('cytomine-client', () => ({
  Cytomine: {
    instance: {host: 'https://test.cytomine.com'}
  },
  AttachedFileCollection: {
    fetchAll: jest.fn().mockResolvedValue({array: []})
  }
}));

describe('attached-files.vue', () => {
  let wrapper;
  let localVue;

  beforeEach(async () => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = shallowMount(attachedFiles, {
      localVue,
      propsData: {
        object: {},
        canEdit: true
      },
      mocks: {
        $t: (message) => message,
        $notify: jest.fn(),
        $buefy: {
          modal: {
            open: jest.fn()
          },
          dialog: {
            confirm: jest.fn((options) => options.onConfirm())
          }
        }
      }
    });

    await wrapper.vm.$nextTick();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render the component correctly', () => {
    expect(wrapper.find('.attached-files-wrapper').exists()).toBe(true);
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.error).toBe(false);
  });

  it('should display no files message when attachedFiles is empty', () => {
    expect(wrapper.text()).toContain('no-attached-file');
    expect(wrapper.vm.attachedFiles).toStrictEqual([]);
  });

  it('should call fetchAll on created', async () => {
    expect(AttachedFileCollection.fetchAll).toHaveBeenCalled();
  });

  it('should open modal on add button click', () => {
    const addButton = wrapper.find('button.button.is-small');
    addButton.trigger('click');

    expect(wrapper.vm.$buefy.modal.open).toHaveBeenCalled();
  });

  it('should add attached file to the list', async () => {
    expect(wrapper.vm.attachedFiles.length).toBe(0);

    wrapper.vm.addAttachedFile({id: 2, filename: 'newfile.pdf'});

    expect(wrapper.vm.attachedFiles.length).toBe(1);
    expect(wrapper.vm.attachedFiles[0].filename).toBe('newfile.pdf');
  });
});
