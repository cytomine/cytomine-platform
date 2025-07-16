<!-- Copyright (c) 2009-2022. Authors: see NOTICE file.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.-->

<template>
<div class="cytomine-quill-editor">
  <div id="tooltip-container"></div> <!-- invisible div defining the allowed positions for ql tooltip -->

  <quill-editor :value="value" @input="$emit('input', $event)" :options="editorOptions" ref="quillEditor">
    <template #toolbar>
      <div id="toolbar">
        <span class="ql-formats">
          <select class="ql-header">
            <option selected> {{ $t('ql-paragraph') }} </option>
            <option value="1"> {{ $t('ql-header-1') }} </option>
            <option value="2"> {{ $t('ql-header-2') }} </option>
            <option value="3"> {{ $t('ql-header-3') }} </option>
            <option value="4"> {{ $t('ql-header-4') }} </option>
          </select>
        </span>

        <span class="ql-formats" :title="$t('ql-font')">
          <select class="ql-font">
            <option selected></option>
            <option value="serif"></option>
            <option value="monospace"></option>
          </select>
        </span>

        <span class="ql-formats">
          <button class="ql-bold" :title="$t('ql-bold')"></button>
          <button class="ql-italic" :title="$t('ql-italic')"></button>
          <button class="ql-underline" :title="$t('ql-underline')"></button>
        </span>

        <!-- Add subscript and superscript buttons -->
        <span class="ql-formats">
          <button class="ql-script" value="sub" :title="$t('ql-subscript')"></button>
          <button class="ql-script" value="super" :title="$t('ql-superscript')"></button>
        </span>

        <span class="ql-formats">
          <span :title="$t('ql-font-color')">
            <select class="ql-color"></select>
          </span>
          <span :title="$t('ql-background-color')">
            <select class="ql-background"></select>
          </span>
        </span>

        <span class="ql-formats" :title="$t('ql-align')">
          <select class="ql-align">
            <option selected></option>
            <option value="center"></option>
            <option value="right"></option>
          </select>
        </span>

        <span class="ql-formats">
          <button class="ql-indent" value="-1" :title="$t('ql-indent-left')"></button>
          <button class="ql-indent" value="+1" :title="$t('ql-indent-right')"></button>
        </span>

        <span class="ql-formats">
          <button class="ql-list" value="ordered" :title="$t('ql-ordered-list')"></button>
          <button class="ql-list" value="bullet" :title="$t('ql-bullet-list')"></button>
        </span>

        <span class="ql-formats">
          <button class="ql-blockquote" :title="$t('ql-blockquote')"></button>
          <button class="ql-code-block" :title="$t('ql-code-block')"></button>
        </span>

        <span class="ql-formats">
          <button class="ql-clean" :title="$t('ql-clean-format')"></button>
        </span>

        <span class="ql-formats">
          <button class="ql-link" :title="$t('ql-link')"></button>
          <button class="ql-image" :title="$t('ql-image')"></button>
          <button class="ql-video" :title="$t('ql-video')"></button>
        </span>

        <span class="ql-formats">
          <span
            :class="['ql-picker', expandedSpecialChars ? 'ql-expanded' : '', 'ql-special-characters']"
            :title="$t('ql-special-character')"
            @click="expandedSpecialChars = !expandedSpecialChars"
            v-click-outside="() => expandedSpecialChars = false"
          >
            <span class="ql-picker-label" tabindex="0" role="button">
              <svg viewBox="0 0 18 18">
                <polygon class="ql-stroke" points="7 11 9 13 11 11 7 11"></polygon>
                <polygon class="ql-stroke" points="7 7 9 5 11 7 7 7"></polygon>
              </svg>
            </span>

            <span class="ql-picker-options">
              <button
                class="special-char"
                v-for="char in specialCharacters" :key="char"
                @click="insertSpecialCharacter(char)"
              >
                {{ char }}
              </button>
            </span>
          </span>
        </span>
      </div>
    </template>
  </quill-editor>
</div>
</template>

<script>
import {quillEditor} from 'vue-quill-editor';

export default {
  name: 'cytomine-quill-editor',
  components: {quillEditor},
  props: {
    value: String,
    placeholder: String
  },
  data() {
    return {
      editorOptions: {
        bounds: '#tooltip-container',
        placeholder: this.placeholder || this.$t('enter-text'),
        modules: {
          toolbar: '#toolbar'
        }
      },
      expandedSpecialChars: false,
      specialCharacters: [
        '$', '€', '£', '¢', '¥', '¤', '‰', '©', '®', '™', '§', '¶', 'Æ', 'æ', 'Œ', 'œ',
        '±', '×', '÷', '⇒', '⇔', '∏', '∑', '≃', '≤', '≥'
      ]
    };
  },
  computed: {
    quill() {
      return this.$refs.quillEditor.quill;
    }
  },
  methods: {
    insertSpecialCharacter(char) {
      let range = this.quill.getSelection(true);
      if (range.length > 0) {
        this.quill.deleteText(range.index, range.length, 'user');
      }
      this.quill.insertText(range.index, char, 'user');
      this.quill.setSelection(range.index + 1);
    }
  }
};
</script>

<style lang="scss">
.cytomine-quill-editor {
  position: relative;
  min-height: 30vh;
  display: flex;
  flex-direction: column;

  .ql-toolbar {
    flex-shrink: 0;
  }

  .ql-bold svg, .ql-italic svg, .ql-underline svg {
    display: none;
  }

  .ql-bold::before {
    content: "A";
    font-weight: 600;
  }

  .ql-italic::before {
    content: "A";
    font-style: italic;
  }

  .ql-underline::before {
    content:"A";
    text-decoration: underline;
  }

  .ql-picker.ql-special-characters .ql-picker-label::before {
    content: "\03A9"; /* Omega */
    font-weight: 600;
    margin-right: 1.5em;
  }

  .ql-picker.ql-special-characters .ql-picker-options  {
    white-space: normal;
    width: 13.3em;
  }

  .special-char {
    display: inline-block !important;
  }

  .special-char:hover {
    background: #f5f5f5 !important;
  }

  #tooltip-container {
    position: absolute;
    left: 1em;
    right: 1em;
    top: 0;
    bottom: 0;
    pointer-events: none; /* to allow selection of elements below it */
  }

  .quill-editor {
    min-height: 30vh;
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  .ql-container {
    overflow: auto;
    flex-grow: 1;
  }
}
</style>
