import {shallowMount} from '@vue/test-utils';

import UploadAppButton from '@/components/appengine/UploadAppButton';
import Task from '@/utils/appengine/task';

jest.mock('@/utils/appengine/task');

describe('UploadAppButton.vue', () => {
  const mockNotify = jest.fn();
  const mockEmit = jest.fn();
  const uploadTaskSpy = jest.spyOn(UploadAppButton.methods, 'uploadTask');
  let wrapper;

  beforeEach(() => {
    uploadTaskSpy.mockClear();

    wrapper = shallowMount(UploadAppButton, {
      propsData: {
        btnFunc: 'upload',
      },
      mocks: {
        $t: msg => msg,
        $notify: mockNotify,
      },
    });

    wrapper.vm.$emit = mockEmit;
    jest.clearAllMocks();
  });

  it('The component should render the upload button with translated label', () => {
    const button = wrapper.find('#app-upload-btn');
    expect(button.exists()).toBe(true);
    expect(button.text()).toBe('upload');
  });

  it('Clicking the button should trigger hidden file input click', async () => {
    await wrapper.find('#app-upload-btn').trigger('click');
    expect(uploadTaskSpy).toHaveBeenCalled();
  });

  it('handleFileChange should call upload with the selected file', async () => {
    const fakeFile = new Blob(['test'], {type: 'text/plain'});
    const mockEvent = {
      target: {
        files: [fakeFile],
      },
    };
    wrapper.vm.upload = jest.fn();
    wrapper.vm.handleFileChange(mockEvent);
    expect(wrapper.vm.upload).toHaveBeenCalledWith(fakeFile);
  });

  it('Upload should emit success and notify on successful upload', async () => {
    Task.uploadTask.mockResolvedValue({status: 200});

    const file = new Blob(['test']);
    await wrapper.vm.upload(file);

    expect(Task.uploadTask).toHaveBeenCalled();
    expect(mockEmit).toHaveBeenCalledWith('taskUploadSuccess');
    expect(mockNotify).toHaveBeenCalledWith({type: 'success', text: 'notify-success-task-upload'});
  });

  it('Upload should notify with warning on 409 error', async () => {
    Task.uploadTask.mockRejectedValue({
      response: {
        status: 409,
        data: {message: 'Task already exists!'},
      },
    });

    const file = new Blob(['test']);
    await wrapper.vm.upload(file);

    expect(mockNotify).toHaveBeenCalledWith({type: 'warn', text: 'Task already exists!'});
  });

  it('Upload should notify with error on generic error', async () => {
    Task.uploadTask.mockRejectedValue({
      response: {
        status: 500,
        data: {message: 'Server error'},
      },
    });

    const file = new Blob(['test']);
    await wrapper.vm.upload(file);

    expect(mockNotify).toHaveBeenCalledWith({type: 'error', text: 'Server error'});
  });
});
