import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';
import VTooltip from 'v-tooltip';

import GlobalDashboard from '@/components/GlobalDashboard';
import {Configuration, ImageInstanceCollection, ProjectCollection} from 'cytomine-client';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

jest.mock('cytomine-client', () => ({
  Configuration: {
    fetch: jest.fn().mockResolvedValue({
      value: 'Welcome to the dashboard!'
    })
  },
  ImageInstanceCollection: {
    fetchLastOpened: jest.fn().mockResolvedValue([
      {id: 1, project: 1}
    ])
  },
  ProjectCollection: {
    fetchAll: jest.fn().mockResolvedValue({
      array: [
        {id: 1, name: 'Project 1', numberOfImages: 10, blindMode: false},
        {id: 2, name: 'Project 2', numberOfImages: 5, blindMode: true}
      ]
    }),
    fetchLastOpened: jest.fn().mockResolvedValue([
      {id: 1},
      {id: 2}
    ])
  }
}));

describe('GlobalDashboard.vue', () => {
  let localVue;
  let wrapper;

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);
    localVue.use(VTooltip);

    wrapper = mount(GlobalDashboard, {
      localVue,
      mocks: {
        $t: (message) => message,
      },
      computed: {
        currentUser: () => ({
          fetchNbAnnotations: jest.fn().mockResolvedValue(5),
        })
      },
      propsData: {
        nbRecent: 3
      },
      stubs: {
        'image-preview': true,
        'list-images-preview': true,
        'router-link': true,
      }
    });
  });

  it('The component should be rendered correctly', () => {
    const headers = wrapper.findAll('h2');

    expect(headers.length).toBe(3);
    expect(headers.at(0).text()).toBe('recently-opened');
    expect(headers.at(1).text()).toBe('statistics');
    expect(headers.at(2).text()).toBe('last-opened-image');

    expect(wrapper.vm.loading).toBe(false);
  });

  it('The component should render the welcome message correctly', () => {
    expect(Configuration.fetch).toHaveBeenCalled();
    expect(wrapper.vm.welcomeMessage).toBe('Welcome to the dashboard!');
    expect(wrapper.find('.box').text()).toContain('Welcome to the dashboard!');
  });

  it('The component should fetch projects and compute statistics', () => {
    expect(ProjectCollection.fetchAll).toHaveBeenCalled();
    expect(wrapper.vm.projects.array.length).toBe(2);
    expect(wrapper.vm.nbImages).toBe(15);
  });

  it('The component should fetch recent projects and display them correctly', async () => {
    expect(ProjectCollection.fetchLastOpened).toHaveBeenCalled();
    expect(wrapper.vm.recentProjects.length).toBe(2);
    expect(wrapper.vm.recentProjects.at(0).name).toBe('Project 1');
  });

  it('The component should fetch the last opened image and bind it to the template', async () => {
    expect(ImageInstanceCollection.fetchLastOpened).toHaveBeenCalled();
    expect(wrapper.vm.lastOpenedImage.projectName).toBe('Project 1');
  });
});
