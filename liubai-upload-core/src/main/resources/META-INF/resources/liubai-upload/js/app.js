new Vue({
    el: '#app',
    data: {
        file: null,
        sha256: null,
        uploading: false,
        progress: 0,
        chunkSize: 1024 * 1024
    },
    methods: {
        handleFileChange(event) {
            this.file = event.target.files[0] || null;
            this.sha256 = null;
            this.progress = 0;
        },

        async startUpload() {
            if (!this.file || this.uploading) {
                return;
            }
            this.uploading = true;
            try {
                this.sha256 = await this.calculateFileSHA256(this.file);
                const response = await axios.get('../file/preprocess', {
                    params: {
                        sha256: this.sha256,
                        totalBytes: this.file.size
                    }
                });
                this.ensureSuccess(response);

                const state = response.data.data;
                if (state.uploadedBytes === this.file.size && state.currentSha256 === this.sha256) {
                    this.progress = 1;
                    alert('文件已存在');
                    return;
                }

                let startByte = 0;
                if (state.uploadedBytes > 0 && state.uploadedBytes < this.file.size) {
                    const uploadedPart = this.file.slice(0, state.uploadedBytes);
                    const localPartialSha256 = await this.calculateFileSHA256(uploadedPart);
                    if (localPartialSha256 === state.currentSha256) {
                        startByte = state.uploadedBytes;
                    }
                }

                await this.uploadChunks(startByte);
                alert('上传成功');
            } catch (error) {
                console.error('上传失败:', error);
                const message = error && error.message ? error.message : '未知错误';
                alert('上传失败：' + message);
            } finally {
                this.uploading = false;
            }
        },

        async uploadChunks(startByte) {
            let offset = startByte;
            this.progress = this.file.size === 0 ? 0 : offset / this.file.size;

            do {
                const end = Math.min(offset + this.chunkSize, this.file.size);
                const chunk = this.file.slice(offset, end);
                const formData = new FormData();
                formData.append('file', chunk, this.file.name);
                formData.append('sha256', this.sha256);
                formData.append('startByte', offset);
                formData.append('totalBytes', this.file.size);

                const response = await axios.post('../file/upload', formData);
                this.ensureSuccess(response);
                offset = end;
                this.progress = this.file.size === 0 ? 1 : offset / this.file.size;
            } while (offset < this.file.size);
        },

        calculateFileSHA256(file) {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                const blockSize = 1024 * 1024;
                const sha256 = CryptoJS.algo.SHA256.create();
                let start = 0;

                reader.onload = event => {
                    sha256.update(CryptoJS.lib.WordArray.create(new Uint8Array(event.target.result)));
                    if (start < file.size) {
                        readNextBlock();
                    } else {
                        resolve(sha256.finalize().toString());
                    }
                };
                reader.onerror = reject;

                const readNextBlock = () => {
                    const end = Math.min(start + blockSize, file.size);
                    reader.readAsArrayBuffer(file.slice(start, end));
                    start = end;
                };
                readNextBlock();
            });
        },

        ensureSuccess(response) {
            if (!response.data || response.data.code !== 20000) {
                throw new Error(response.data && response.data.message
                    ? response.data.message
                    : '服务端返回异常');
            }
        }
    }
});
