data_file_dir = '../../../cases/Filtered-Correlation';
files = dir([data_file_dir, '/*.log']);
n_file = length(files);
for i_file = 1:n_file
    data_file_name = replace(files(i_file).name, '.log', '.dat');
    plot_orbit_count(data_file_dir, data_file_name);
    pause;
    close();

end % for
